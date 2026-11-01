package com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService.Outcome;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantDailySummaryScheduler")
class AssistantDailySummarySchedulerTest {

    private static final String FIRST = "0b6a5a3e-5c4f-4b8e-9a35-4b7e3e5f1a10";
    private static final String SECOND = "1c7b6b4f-6d5a-4c9f-8b46-5c8f4f6a2b21";
    private static final String THIRD = "2d8c7c5a-7e6b-4dab-9c57-6d9a5a7b3c32";

    @Mock private ExternalOrchardService orchard;
    @Mock private DailySummaryCommandService summaries;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AssistantDailySummaryScheduler.class);

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
    }

    @Test
    @DisplayName("prepares the field summary of every producer with active plots, in Spanish, as the job (no limits)")
    void precomputesEveryProducer() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of(FIRST, SECOND));
        when(summaries.handle(any())).thenReturn(outcome(true), outcome(false));

        var run = scheduler(1000).precompute();

        assertThat(run).isEqualTo(new AssistantDailySummaryScheduler.Run(1, 1, 0, 0));
        var commands = ArgumentCaptor.forClass(PrepareDailySummaryCommand.class);
        verify(summaries, times(2)).handle(commands.capture());
        assertThat(commands.getAllValues()).containsExactly(
                new PrepareDailySummaryCommand(FIRST, null, AnswerLanguage.SPANISH, true),
                new PrepareDailySummaryCommand(SECOND, null, AnswerLanguage.SPANISH, true));
    }

    @Test
    @DisplayName("one producer failing or refused never stops the rest, and the logs carry no identifier")
    void survivesFailuresWithoutIdentifiersInLogs() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of(FIRST, SECOND, THIRD));
        when(summaries.handle(any())).thenAnswer(invocation -> {
            var producer = invocation.<PrepareDailySummaryCommand>getArgument(0).producerId();
            if (producer.equals(FIRST)) {
                throw new IllegalStateException("plot " + FIRST + " of producer " + FIRST + " failed");
            }
            return producer.equals(SECOND)
                    ? Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"))
                    : outcome(true);
        });

        var run = scheduler(1000).precompute();

        assertThat(run).isEqualTo(new AssistantDailySummaryScheduler.Run(1, 0, 1, 1));
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Assistant daily summary of a producer failed: IllegalStateException")
                .allSatisfy(message -> assertThat(message).doesNotContain(FIRST, SECOND, THIRD));
    }

    @Test
    @DisplayName("stops at the configured number of producers; a failing producer listing ends the run quietly")
    void bounded() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of(FIRST, SECOND, THIRD));
        when(summaries.handle(any())).thenReturn(outcome(true));

        assertThat(scheduler(2).precompute()).isEqualTo(new AssistantDailySummaryScheduler.Run(2, 0, 0, 0));
        verify(summaries, times(2)).handle(any());

        when(orchard.findProducersWithActivePlots()).thenThrow(new IllegalStateException("producer " + FIRST));
        assertThat(scheduler(2).precompute()).isEqualTo(new AssistantDailySummaryScheduler.Run(0, 0, 0, 0));
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Assistant daily summaries not prepared: IllegalStateException")
                .allSatisfy(message -> assertThat(message).doesNotContain(FIRST));
        assertThatThrownBy(() -> scheduler(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("runs once a day at 06:30 in Lima, on by default in production and off in the tests")
    void limaScheduleOnInProductionOffInTests() throws Exception {
        var scheduled = AssistantDailySummaryScheduler.class.getMethod("run").getAnnotation(Scheduled.class);
        var condition = AssistantDailySummaryScheduler.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(scheduled.zone()).isEqualTo("America/Lima");
        assertThat(scheduled.cron()).isEqualTo("${viora.assistant.daily-summary.cron:0 30 6 * * *}");
        assertThat(condition.name()).containsExactly("viora.assistant.daily-summary.enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
        var production = properties("/application.properties");
        assertThat(production.getProperty("viora.assistant.daily-summary.cron"))
                .isEqualTo("${VIORA_ASSISTANT_DAILY_SUMMARY_CRON:0 30 6 * * *}");
        assertThat(production.getProperty("viora.assistant.daily-summary.enabled"))
                .isEqualTo("${VIORA_ASSISTANT_DAILY_SUMMARY_ENABLED:true}");
        assertThat(properties("/config/application.properties").getProperty("viora.assistant.daily-summary.enabled"))
                .isEqualTo("false");
    }

    private AssistantDailySummaryScheduler scheduler(int maxProducers) {
        return new AssistantDailySummaryScheduler(orchard, summaries, maxProducers);
    }

    private static Result<Outcome, ApplicationError> outcome(boolean created) {
        return Result.success(new Outcome(mock(DailySummary.class), created));
    }

    private Properties properties(String resource) throws Exception {
        var properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            properties.load(in);
        }
        return properties;
    }
}
