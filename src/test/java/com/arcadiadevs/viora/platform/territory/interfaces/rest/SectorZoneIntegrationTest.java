package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RemovePlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.purge.TerritoryAccountDataPurger;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories.PlotSectorAssignmentPersistenceRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sector list of a license and sectors of member plots, against the whole application: real manager, members,
 * sponsored plans and plots.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territorysectorzones;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Sector zones integration")
class SectorZoneIntegrationTest {

    private static final String ZONES = "/api/v1/sector-zones";
    private static final String PLOTS = "/api/v1/plot-sector-assignments";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired PlotCommandService plotCommandService;
    @Autowired PlotSectorAssignmentPersistenceRepository assignmentRows;
    @Autowired TerritoryAccountDataPurger purger;

    private MockMvc mvc;
    private String manager;
    private String member;
    private String plot;
    private CooperativeLicense license;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        license = licenseOf(manager);
        member = sponsoredMember(license, "Julio Quispe");
        plot = newPlot(member, "Parcela 3");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The manager adds, lists and renames sectors; accents and case do not make a new one")
    void managerKeepsTheList() throws Exception {
        authenticateAs(manager);

        var lowerYarada = addZone("La Yarada Baja")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/sector-zones/")))
                .andExpect(jsonPath("$.name").value("La Yarada Baja"))
                .andExpect(jsonPath("$.plotCount").value(0));
        addZone("la  yarada baja")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("territory.sector_zone.name_taken"));
        var magollo = idOf(addZone("Magollo").andExpect(status().isCreated()));

        mvc.perform(get(ZONES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("La Yarada Baja"))
                .andExpect(jsonPath("$[1].name").value("Magollo"));
        rename(magollo, "Magollo Alto").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Magollo Alto"));
        rename(magollo, "LA YARADA BAJA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("territory.sector_zone.name_taken"));
        rename(magollo, "magollo alto").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("magollo alto"));
        assertThat(idOf(lowerYarada)).isNotBlank();
    }

    @Test
    @DisplayName("A member picks a sector of the list; the manager sees the plot, its owner and the count")
    void memberChoosesFromTheList() throws Exception {
        authenticateAs(manager);
        var losPalos = idOf(addZone("Los Palos"));

        authenticateAs(member);
        mvc.perform(get(ZONES)).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Los Palos"));
        assign(plot, "{\"sectorZoneId\": \"" + losPalos + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.sectorZoneName").value("Los Palos"))
                .andExpect(jsonPath("$.plotName").value("Parcela 3"));
        mvc.perform(get(PLOTS)).andExpect(jsonPath("$.length()").value(1));

        authenticateAs(manager);
        mvc.perform(get(ZONES + "/" + losPalos)).andExpect(jsonPath("$.plotCount").value(1));
        mvc.perform(get(PLOTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plotId").value(plot))
                .andExpect(jsonPath("$[0].producerName").value("Julio Quispe"))
                .andExpect(jsonPath("$[0].sectorZoneName").value("Los Palos"));
    }

    @Test
    @DisplayName("A written name that matches the list, ignoring accents and case, takes that sector")
    void writtenNameMatchesTheList() throws Exception {
        authenticateAs(manager);
        addZone("Cerro Blanco");

        authenticateAs(member);
        assign(plot, "{\"name\": \"cerro BLANCO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.sectorZoneName").value("Cerro Blanco"));
    }

    @Test
    @DisplayName("A new written name waits «Por confirmar»; adding it as a sector resolves every plot with that name")
    void addingAWrittenNameResolvesEveryPlotWithIt() throws Exception {
        authenticateAs(member);
        assign(plot, "{\"name\": \"Yarada Los Palos\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proposedName").value("Yarada Los Palos"))
                .andExpect(jsonPath("$.sectorZoneId").doesNotExist());
        var other = sponsoredMember(license, "Ángel Choque");
        var otherPlot = newPlot(other, "Lote 1");
        authenticateAs(other);
        assign(otherPlot, "{\"name\": \"yarada los palos\"}").andExpect(jsonPath("$.status").value("PENDING"));

        authenticateAs(manager);
        mvc.perform(get(PLOTS).param("status", "pending")).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post(PLOTS + "/" + plot + "/resolutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.sectorZoneName").value("Yarada Los Palos"));

        mvc.perform(get(PLOTS).param("status", "pending")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(ZONES))
                .andExpect(jsonPath("$[0].name").value("Yarada Los Palos"))
                .andExpect(jsonPath("$[0].plotCount").value(2));
        mvc.perform(post(PLOTS + "/" + otherPlot + "/resolutions"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("territory.plot_sector.not_pending"));
    }

    @Test
    @DisplayName("«Es el mismo que…» joins a plot «Por confirmar» to an existing sector")
    void pendingJoinsAnExistingSector() throws Exception {
        authenticateAs(manager);
        var losPalos = idOf(addZone("Los Palos"));
        authenticateAs(member);
        assign(plot, "{\"name\": \"Yarada Los Palos\"}");

        authenticateAs(manager);
        mvc.perform(post(PLOTS + "/" + plot + "/resolutions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectorZoneId\": \"" + losPalos + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectorZoneId").value(losPalos))
                .andExpect(jsonPath("$.sectorZoneName").value("Los Palos"));
        mvc.perform(get(ZONES)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Invalid choices, other people's plots and other licenses' sectors are refused")
    void refusals() throws Exception {
        var otherManager = signUp("ROLE_TECHNICAL_MANAGER", "Rosa Condori");
        var otherLicense = licenseOf(otherManager);
        authenticateAs(otherManager);
        var foreignZone = idOf(addZone("Sama"));
        authenticateAs(manager);
        var sama = idOf(addZone("Sama"));

        authenticateAs(member);
        assign(plot, "{}").andExpect(status().isBadRequest());
        assign(plot, "{\"sectorZoneId\": \"" + sama + "\", \"name\": \"Sama\"}").andExpect(status().isBadRequest());
        assign(plot, "{\"name\": \" \"}").andExpect(status().isBadRequest());
        assign(plot, "{\"sectorZoneId\": \"" + foreignZone + "\"}").andExpect(status().isNotFound());
        var stranger = sponsoredMember(otherLicense, "Hilda Poma");
        assign(newPlot(stranger, "Lote Sur"), "{\"sectorZoneId\": \"" + sama + "\"}").andExpect(status().isNotFound());
        addZone("Magollo").andExpect(status().isForbidden());
        mvc.perform(get(PLOTS).param("status", "lost")).andExpect(status().isBadRequest());

        authenticateAs(signUp("ROLE_PRODUCER", "Sin Plan"));
        mvc.perform(get(ZONES))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("territory.producer.not_sponsored"));
        authenticateAs(signUp("ROLE_TECHNICAL_MANAGER", "Sin Licencia"));
        mvc.perform(get(ZONES))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("territory.manager.no_license"));

        authenticateAs(member);
        assign(plot, "{\"name\": \"Cerro Blanco\"}");
        authenticateAs(otherManager);
        mvc.perform(post(PLOTS + "/" + plot + "/resolutions")).andExpect(status().isNotFound());
        rename(sama, "Sama Alta").andExpect(status().isNotFound());
        mvc.perform(get(PLOTS + "/" + plot)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An archived plot drops out of the lists and counts, and a deleted account loses its plot sectors")
    void archivedPlotsAndDeletedAccounts() throws Exception {
        authenticateAs(manager);
        var losPalos = idOf(addZone("Los Palos"));
        authenticateAs(member);
        assign(plot, "{\"sectorZoneId\": \"" + losPalos + "\"}").andExpect(status().isOk());
        var second = newPlot(member, "Parcela 4");
        assign(second, "{\"sectorZoneId\": \"" + losPalos + "\"}").andExpect(status().isOk());

        assertThat(plotCommandService.handle(new RemovePlotCommand(plot, member, "test")).isSuccess()).isTrue();

        authenticateAs(manager);
        mvc.perform(get(ZONES + "/" + losPalos)).andExpect(jsonPath("$.plotCount").value(1));
        mvc.perform(get(PLOTS)).andExpect(jsonPath("$.length()").value(1));

        purger.on(new UserAccountDeletedEvent(member, "deleted@viora.ag", Instant.now()));

        assertThat(assignmentRows.findAll())
                .noneMatch(row -> row.getProducerId().toString().equals(member));
        mvc.perform(get(ZONES + "/" + losPalos)).andExpect(jsonPath("$.plotCount").value(0));
    }

    private ResultActions addZone(String name) throws Exception {
        return mvc.perform(post(ZONES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }

    private ResultActions rename(String id, String name) throws Exception {
        return mvc.perform(patch(ZONES + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }

    private ResultActions assign(String plotId, String body) throws Exception {
        return mvc.perform(put(PLOTS + "/" + plotId).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions actions) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private CooperativeLicense licenseOf(String managerId) {
        return licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)),
                        Instant.now().plus(Duration.ofDays(120))),
                new ManagerId(managerId), 0L)));
    }

    private String sponsoredMember(CooperativeLicense owner, String fullName) {
        var producer = signUp("ROLE_PRODUCER", fullName);
        var code = randomCode();
        var stored = licenseRepository.findById(owner.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(), new HectaresQuota(new BigDecimal("12")),
                Instant.now().plus(Duration.ofDays(7)));
        licenseRepository.save(stored);
        invitationCodeRepository.save(issued);
        assertThat(redemptionCommandService.handle(new RedeemCooperativeCodeCommand(producer, code)).isSuccess())
                .isTrue();
        return producer;
    }

    private String newPlot(String producerId, String name) {
        var lng = -70.25 + RANDOM.nextInt(1000) * 0.002;
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
