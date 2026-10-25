package com.arcadiadevs.viora.platform.subscription.interfaces;
import java.math.BigDecimal;
import java.time.Instant;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;

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
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private SubscriptionUnitOfWork work;
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
                .andExpect(jsonPath("$.totalAmount").value(330.0))
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
    void anUnpaidPurchaseTakesTheHectaresOfANewSelection() throws Exception {
        var first = mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "half")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.5}"))
                .andExpect(status().isCreated()).andReturn();
        var id = com.jayway.jsonpath.JsonPath.<String>read(first.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "two")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.quotaHa").value(2.0))
                .andExpect(jsonPath("$.totalAmount").value(1320.0))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "two-again")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
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
        var command = new CreateSubscriptionCommand(owner, "PRODUCER_ANNUAL", new BigDecimal("0.5"), "concurrent");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<UUID> call = () -> { start.await(); return commands.handle(command).success().orElseThrow().id(); };
            var a = pool.submit(call);
            var b = pool.submit(call);
            start.countDown();
            assertEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0.5,279.66,50.34,330.00", "1,559.32,100.68,660.00",
            "10,5593.22,1006.78,6600.00", "12,6711.86,1208.14,7920.00"})
    void persistsAndReadsTheSameInclusiveAnnualPrice(String quota, String base, String tax, String total) throws Exception {
        var response = mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "inclusive")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":" + quota + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseAmount").value(Double.valueOf(base)))
                .andExpect(jsonPath("$.taxAmount").value(Double.valueOf(tax)))
                .andExpect(jsonPath("$.totalAmount").value(Double.valueOf(total))).andReturn();
        var id = com.jayway.jsonpath.JsonPath.<String>read(response.getResponse().getContentAsString(), "$.id");
        var saved = subscriptions.findById(new SubscriptionId(id)).orElseThrow();
        assertEquals(new BigDecimal(total), saved.snapshot().price().total().amount());
        mvc.perform(get("/api/v1/subscriptions/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAmount").value(Double.valueOf(base)))
                .andExpect(jsonPath("$.taxAmount").value(Double.valueOf(tax)))
                .andExpect(jsonPath("$.totalAmount").value(Double.valueOf(total)));
    }

    @Test
    void replayOfLegacyPurchaseKeepsItsOriginalPersistedPrice() throws Exception {
        var oldPrice = new SubscriptionPrice(
                new Money(new BigDecimal("330.00")),
                new Money(new BigDecimal("59.40")),
                new Money(new BigDecimal("389.40")));
        var contract = subscriptions.save(Subscription.createIndividual(
                new ProducerId(owner), "PRODUCER_ANNUAL",
                new IndividualHectaresQuota(new BigDecimal("0.5")),
                oldPrice, Instant.now().minusSeconds(60)));
        var id = UUID.fromString(contract.snapshot().id().subscriptionId());
        work.exclusive(owner, () -> {
            work.recordOperation(owner, "legacy", "CREATE_SUBSCRIPTION", "PRODUCER_ANNUAL:0.5", id);
            return id;
        });
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "legacy")
                .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.baseAmount").value(330.0))
                .andExpect(jsonPath("$.taxAmount").value(59.4))
                .andExpect(jsonPath("$.totalAmount").value(389.4));
        assertEquals(oldPrice, subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot().price());
    }
}
