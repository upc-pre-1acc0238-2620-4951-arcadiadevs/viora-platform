package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "viora.security.demo.enabled=true")
class IndividualContractCreationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private SubscriptionCommandService commands;
    @MockitoBean private ProducerProfilePort profiles;
    private MockMvc mvc;
    private String owner;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        owner = UUID.randomUUID().toString();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner, null, List.of()));
        when(profiles.isReady(anyString())).thenReturn(true);
    }

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsServerPricedPendingContractAndReplaysOperation() throws Exception {
        var request = "{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.5,\"totalAmount\":1}";
        var first = mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "create-1")
                .contentType("application/json").content(request)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(389.4))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.entitlementActive").value(false)).andReturn();
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "create-1")
                .contentType("application/json").content(request)).andExpect(status().isOk())
                .andExpect(header().string("Location", first.getResponse().getHeader("Location")));
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "create-1")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAnonymousEvenWhenDemoFallbackExists() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "anonymous")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingProfileAndInvalidQuota() throws Exception {
        when(profiles.isReady(owner)).thenReturn(false);
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "no-profile")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.5}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "invalid")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.7}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void concurrentOperationReplaysOneDurableContract() throws Exception {
        var command = new CreateSubscriptionCommand(owner, "PRODUCER_ANNUAL", new java.math.BigDecimal("0.5"), "concurrent");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<UUID> call = () -> { start.await(); return commands.handle(command).success().orElseThrow().id(); };
            var a = pool.submit(call);
            var b = pool.submit(call);
            start.countDown();
            assertEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }
}
