package com.arcadiadevs.viora.platform.territory.application;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorAlertCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.commandservices.SectorAlertEvaluator;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SectorAlertEvaluator Unit Tests")
class SectorAlertEvaluatorTest {

    private final String license = UUID.randomUUID().toString();
    private final String otherLicense = UUID.randomUUID().toString();

    @Test
    @DisplayName("Evaluations of the same license never overlap")
    void sameLicenseIsSerialized() throws Exception {
        var service = new OverlapRecordingService(null);
        var evaluator = new SectorAlertEvaluator(service);

        var results = runAtOnce(6, () -> evaluator.evaluate(new LicenseId(license)));

        assertThat(results).allMatch(Result::isSuccess);
        assertThat(service.calls.get()).isEqualTo(6);
        assertThat(service.mostAtOnce.get(license)).isEqualTo(1);
    }

    @Test
    @DisplayName("Different licenses are evaluated at the same time")
    void otherLicensesDoNotWait() throws Exception {
        // The first license only finishes once the second one is being evaluated.
        var otherStarted = new CountDownLatch(1);
        var service = new OverlapRecordingService(otherStarted);
        var evaluator = new SectorAlertEvaluator(service);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> evaluator.evaluate(new LicenseId(license)));
            var second = executor.submit(() -> evaluator.evaluate(new LicenseId(otherLicense)));

            assertThat(first.get(5, TimeUnit.SECONDS).isSuccess()).isTrue();
            assertThat(second.get(5, TimeUnit.SECONDS).isSuccess()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Without a license there is nothing to evaluate")
    void nullLicenseFails() {
        var service = new OverlapRecordingService(null);

        var result = new SectorAlertEvaluator(service).evaluate(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(service.calls.get()).isZero();
    }

    private static List<Result<Integer, ApplicationError>> runAtOnce(
            int times, java.util.concurrent.Callable<Result<Integer, ApplicationError>> task) throws Exception {
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(times);
        try {
            var futures = new ArrayList<Future<Result<Integer, ApplicationError>>>();
            for (var index = 0; index < times; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            var results = new ArrayList<Result<Integer, ApplicationError>>();
            for (var future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /** Records how many evaluations of each license run at the same time. */
    private final class OverlapRecordingService implements SectorAlertCommandService {

        private final CountDownLatch otherStarted;
        private final ConcurrentHashMap<String, AtomicInteger> running = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Integer> mostAtOnce = new ConcurrentHashMap<>();
        private final AtomicInteger calls = new AtomicInteger();

        private OverlapRecordingService(CountDownLatch otherStarted) {
            this.otherStarted = otherStarted;
        }

        @Override
        public Result<Integer, ApplicationError> handle(EvaluateSectorAlertsCommand command) {
            calls.incrementAndGet();
            var now = running.computeIfAbsent(command.licenseId(), id -> new AtomicInteger()).incrementAndGet();
            mostAtOnce.merge(command.licenseId(), now, Math::max);
            try {
                if (otherStarted == null) {
                    Thread.sleep(30);
                } else if (command.licenseId().equals(otherLicense)) {
                    otherStarted.countDown();
                } else if (!otherStarted.await(3, TimeUnit.SECONDS)) {
                    return Result.failure(ApplicationError.validationError("licenseId", "waited.for.other"));
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                running.get(command.licenseId()).decrementAndGet();
            }
            return Result.success(0);
        }
    }
}
