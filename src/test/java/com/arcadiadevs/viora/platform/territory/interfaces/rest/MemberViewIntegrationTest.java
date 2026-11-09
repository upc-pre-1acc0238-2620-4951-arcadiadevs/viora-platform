package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.DataSharingConsentCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.WithdrawDataSharingConsentCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.DataSharingConsentPersistenceRepository;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The manager's read-only view of their members and member plots, against the whole application: members who share,
 * who stopped sharing and who never shared, with real plots and sectors.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territorymemberview;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Manager read-only view integration")
class MemberViewIntegrationTest {

    private static final String MEMBERS = "/api/v1/cooperative-members";
    private static final String PLOTS = "/api/v1/member-plots";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired DataSharingConsentCommandService consentCommandService;
    @Autowired DataSharingConsentPersistenceRepository consentRows;
    @Autowired PlotCommandService plotCommandService;
    @Autowired SectorZoneCommandService sectorZoneCommandService;
    @Autowired PlotSectorAssignmentCommandService plotSectorAssignmentCommandService;

    private MockMvc mvc;
    private String manager;
    private CooperativeLicense license;
    private String sharing;
    private String sharingPlot;
    private String withdrawn;
    private String withdrawnPlot;
    private String silent;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        license = licenseOf(manager);
        sharing = member(license, "Teodoro Mamani", true);
        sharingPlot = newPlot(sharing, "La Yarada 02", -70.25);
        newPlot(sharing, "Lote Norte", -70.30);
        withdrawn = member(license, "Hilda Poma", true);
        withdrawnPlot = newPlot(withdrawn, "Lote Sur", -70.27);
        var consent = consentRows.findAllByProducerIdOrderByAcceptedAtDesc(UUID.fromString(withdrawn)).getFirst();
        assertThat(consentCommandService.handle(new WithdrawDataSharingConsentCommand(withdrawn,
                consent.getId().toString())).isSuccess()).isTrue();
        silent = member(license, "Ángel Choque", false);
        newPlot(silent, "Lote 1", -70.28);
        var zone = sectorZoneCommandService.handle(new CreateSectorZoneCommand(manager, "La Yarada Baja"))
                .success().orElseThrow().snapshot().id().sectorZoneId();
        assertThat(plotSectorAssignmentCommandService.handle(
                new AssignPlotSectorCommand(sharing, sharingPlot, zone, null)).isSuccess()).isTrue();
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The roll lists every member with whether they share; only who shares shows plots and hectares")
    void rollShowsEachMemberState() throws Exception {
        mvc.perform(get(MEMBERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Hilda Poma"))
                .andExpect(jsonPath("$[0].sharing").value("WITHDRAWN"))
                .andExpect(jsonPath("$[0].withdrawnAt").isNotEmpty())
                .andExpect(jsonPath("$[0].plotCount").doesNotExist())
                .andExpect(jsonPath("$[1].fullName").value("Teodoro Mamani"))
                .andExpect(jsonPath("$[1].sharing").value("SHARING"))
                .andExpect(jsonPath("$[1].plotCount").value(2))
                .andExpect(jsonPath("$[1].registeredAreaHa").isNumber())
                .andExpect(jsonPath("$[1].sectorNames[0]").value("La Yarada Baja"))
                .andExpect(jsonPath("$[1].worstRiskLevel").value("NO_DATA"))
                .andExpect(jsonPath("$[2].fullName").value("Ángel Choque"))
                .andExpect(jsonPath("$[2].sharing").value("NOT_SHARING"))
                .andExpect(jsonPath("$[2].plotCount").doesNotExist());
    }

    @Test
    @DisplayName("A member who shares shows contact, plan, masked code and plots; one who stopped, only name and date")
    void memberDetailRespectsSharing() throws Exception {
        mvc.perform(get(MEMBERS + "/" + sharing))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+51951406118"))
                .andExpect(jsonPath("$.email").value(containsString("@viora.ag")))
                .andExpect(jsonPath("$.quotaHa").value(12.0))
                .andExpect(jsonPath("$.maskedCode").value(containsString("COOP-••")))
                .andExpect(jsonPath("$.memberSince").isNotEmpty())
                .andExpect(jsonPath("$.plots.length()").value(2));

        mvc.perform(get(MEMBERS + "/" + withdrawn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharing").value("WITHDRAWN"))
                .andExpect(jsonPath("$.withdrawnAt").isNotEmpty())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.plots.length()").value(0))
                .andExpect(content().string(not(containsString("Lote Sur"))));
        mvc.perform(get(MEMBERS + "/" + silent))
                .andExpect(jsonPath("$.sharing").value("NOT_SHARING"))
                .andExpect(jsonPath("$.phone").doesNotExist());
        mvc.perform(get(MEMBERS + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The map lists only plots of members who share, with sector, color, polygon and distance")
    void mapShowsOnlySharedPlots() throws Exception {
        mvc.perform(get(PLOTS).param("latitude", "-18.0505").param("longitude", "-70.2495"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].plotId").value(sharingPlot))
                .andExpect(jsonPath("$[0].producerName").value("Teodoro Mamani"))
                .andExpect(jsonPath("$[0].sectorZoneName").value("La Yarada Baja"))
                .andExpect(jsonPath("$[0].sectorStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[0].riskLevel").value("NO_DATA"))
                .andExpect(jsonPath("$[0].overloadPercent").doesNotExist())
                .andExpect(jsonPath("$[0].geoJson").value(containsString("Polygon")))
                .andExpect(jsonPath("$[0].distanceKm").isNumber())
                .andExpect(jsonPath("$[1].plotName").value("Lote Norte"))
                .andExpect(jsonPath("$[1].sectorStatus").value("NONE"))
                .andExpect(jsonPath("$[0].evaluatedAt").isNotEmpty());

        mvc.perform(get(PLOTS)).andExpect(jsonPath("$[0].distanceKm").doesNotExist());
        mvc.perform(get(PLOTS).param("latitude", "-18.05")).andExpect(status().isBadRequest());
        mvc.perform(get(PLOTS).param("latitude", "-95").param("longitude", "-70")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A plot reads in detail only while its owner shares; nothing private of the producer comes out")
    void plotDetail() throws Exception {
        mvc.perform(get(PLOTS + "/" + sharingPlot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotName").value("La Yarada 02"))
                .andExpect(jsonPath("$.producerPhone").value("+51951406118"))
                .andExpect(jsonPath("$.riskLevel").value("NO_DATA"))
                .andExpect(jsonPath("$.sampledTrees").value(0))
                .andExpect(jsonPath("$.sectorZoneName").value("La Yarada Baja"))
                .andExpect(content().string(not(containsString("fieldNote"))))
                .andExpect(content().string(not(containsString("payment"))));

        mvc.perform(get(PLOTS + "/" + withdrawnPlot)).andExpect(status().isNotFound());
        mvc.perform(get(PLOTS + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Another manager, a manager without license and a producer reach nothing of this license")
    void otherUsersReachNothing() throws Exception {
        var otherManager = signUp("ROLE_TECHNICAL_MANAGER", "Rosa Condori");
        licenseOf(otherManager);
        authenticateAs(otherManager);
        mvc.perform(get(MEMBERS)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(MEMBERS + "/" + sharing)).andExpect(status().isNotFound());
        mvc.perform(get(PLOTS + "/" + sharingPlot)).andExpect(status().isNotFound());

        authenticateAs(signUp("ROLE_TECHNICAL_MANAGER", "Sin Licencia"));
        mvc.perform(get(PLOTS))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("territory.manager.no_license"));

        authenticateAs(sharing);
        mvc.perform(get(MEMBERS)).andExpect(status().isForbidden());
        mvc.perform(get(PLOTS)).andExpect(status().isForbidden());
    }

    private CooperativeLicense licenseOf(String managerId) {
        return licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)),
                        Instant.now().plus(Duration.ofDays(120))),
                new ManagerId(managerId), 0L)));
    }

    private String member(CooperativeLicense owner, String fullName, boolean shares) {
        var producer = signUp("ROLE_PRODUCER", fullName);
        var code = randomCode();
        var stored = licenseRepository.findById(owner.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(), new HectaresQuota(new BigDecimal("12")),
                Instant.now().plus(Duration.ofDays(7)));
        licenseRepository.save(stored);
        invitationCodeRepository.save(issued);
        var command = new RedeemCooperativeCodeCommand(producer, code, "2026-11-v1");
        assertThat(redemptionCommandService.handle(command).isSuccess()).isTrue();
        if (!shares) {
            // A plan redeemed before the consent became mandatory has none.
            consentRows.deleteAll(consentRows.findAllByProducerIdOrderByAcceptedAtDesc(UUID.fromString(producer)));
        }
        return producer;
    }

    private String newPlot(String producerId, String name, double lng) {
        var geometry = "{\"type\":\"Polygon\",\"coordinates\":[[[" + lng + ",-18.05],[" + (lng + 0.001) + ",-18.05],["
                + (lng + 0.001) + ",-18.051],[" + lng + ",-18.051],[" + lng + ",-18.05]]]}";
        var result = plotCommandService.handle(new DelimitPlotCommand(producerId, name, "SEVILLANA", geometry, 7.0,
                7.0));
        assertThat(result.isSuccess()).as(String.valueOf(result.failure())).isTrue();
        return result.success().orElseThrow();
    }

    private static String randomCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var code = new StringBuilder("COOP-");
        for (var index = 0; index < 4; index++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    private String signUp(String role, String fullName) {
        var email = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@viora.ag";
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                role, true, fullName, "PE", "+51951406118"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
