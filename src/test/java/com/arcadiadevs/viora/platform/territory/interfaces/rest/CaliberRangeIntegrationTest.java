package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caliber ranges of a license, against the whole application: a real manager and license.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territorycaliberranges;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Caliber ranges integration")
class CaliberRangeIntegrationTest {

    private static final String RANGES = "/api/v1/caliber-ranges";

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        licenseOf(manager);
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A license starts without ranges; the manager sets them and reads them back in order")
    void managerSetsTheRanges() throws Exception {
        mvc.perform(get(RANGES)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        replace("[{\"minFruitsPerKg\":140,\"maxFruitsPerKg\":180},{\"minFruitsPerKg\":181,\"maxFruitsPerKg\":220},"
                + "{\"minFruitsPerKg\":221,\"maxFruitsPerKg\":260}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].minFruitsPerKg").value(181));
        mvc.perform(get(RANGES))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].maxFruitsPerKg").value(260));

        replace("[]").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Overlapping, gapped or inverted ranges answer 400 naming the range, and the ranges in force stay")
    void conflictingRanges() throws Exception {
        replace("[{\"minFruitsPerKg\":140,\"maxFruitsPerKg\":180}]").andExpect(status().isOk());

        replace("[{\"minFruitsPerKg\":140,\"maxFruitsPerKg\":180},{\"minFruitsPerKg\":170,\"maxFruitsPerKg\":220}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("territory.caliber_range.overlapping"))
                .andExpect(jsonPath("$.rangeIndex").value(1));
        replace("[{\"minFruitsPerKg\":140,\"maxFruitsPerKg\":180},{\"minFruitsPerKg\":200,\"maxFruitsPerKg\":220}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("territory.caliber_range.gap"));
        replace("[{\"minFruitsPerKg\":220,\"maxFruitsPerKg\":180}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("territory.caliber_range.inverted"))
                .andExpect(jsonPath("$.rangeIndex").value(0));
        replace("[{\"minFruitsPerKg\":140}]").andExpect(status().isBadRequest());

        mvc.perform(get(RANGES))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].maxFruitsPerKg").value(180));
    }

    private ResultActions replace(String ranges) throws Exception {
        return mvc.perform(put(RANGES).contentType(MediaType.APPLICATION_JSON).content("{\"ranges\":" + ranges + "}"));
    }

    private void licenseOf(String managerId) {
        licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)),
                        Instant.now().plus(Duration.ofDays(120))),
                new ManagerId(managerId), 0L)));
    }

    @SuppressWarnings("unchecked")
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
