package com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantDailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AssistantDailySummaryScheduler")
class AssistantDailySummarySchedulerTest {

    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final AssistantDailySummaryCommandService summaries = mock(AssistantDailySummaryCommandService.class);

    @Test
    @DisplayName("Precomputes the field summary of every producer with active plots, in Spanish, without limits")
    void precomputesEveryProducer() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of("p-1", "p-2", "p-3"));
        when(summaries.handle(any())).thenReturn(outcome(true));
        when(summaries.handle(argThat(command -> command != null && command.producerId().equals("p-2")))).thenReturn(outcome(false));

        var run = scheduler(10).precompute();

        var commands = ArgumentCaptor.forClass(PrepareDailySummaryCommand.class);
        verify(summaries, times(3)).handle(commands.capture());
        assertThat(commands.getAllValues()).extracting(PrepareDailySummaryCommand::producerId)
                .containsExactly("p-1", "p-2", "p-3");
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.plotId()).isNull();
            assertThat(command.locale()).isEqualTo(Locale.forLanguageTag("es"));
            assertThat(command.scheduled()).isTrue();
        });
        assertThat(run).isEqualTo(new AssistantDailySummaryScheduler.Run(2, 1, 0, 0));
    }

    @Test
    @DisplayName("One producer failing or refused never stops the rest")
    void survivesFailures() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of("p-1", "p-2", "p-3", "p-4"));
        when(summaries.handle(any())).thenReturn(outcome(true));
        when(summaries.handle(argThat(command -> command != null && command.producerId().equals("p-1"))))
                .thenThrow(new IllegalStateException("facade down"));
        when(summaries.handle(argThat(command -> command != null && command.producerId().equals("p-3"))))
                .thenReturn(Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive")));

        var run = scheduler(10).precompute();

        verify(summaries, times(4)).handle(any());
        assertThat(run).isEqualTo(new AssistantDailySummaryScheduler.Run(2, 0, 1, 1));
    }

    @Test
    @DisplayName("Stops at the configured number of producers; a failing producer listing ends the run quietly")
    void bounded() {
        when(orchard.findProducersWithActivePlots()).thenReturn(List.of("p-1", "p-2", "p-3"));
        when(summaries.handle(any())).thenReturn(outcome(true));

        assertThat(scheduler(2).precompute()).isEqualTo(new AssistantDailySummaryScheduler.Run(2, 0, 0, 0));
        verify(summaries, times(2)).handle(any());

        when(orchard.findProducersWithActivePlots()).thenThrow(new IllegalStateException("orchard down"));
        assertThat(scheduler(2).precompute()).isEqualTo(new AssistantDailySummaryScheduler.Run(0, 0, 0, 0));
    }

    @Test
    @DisplayName("Runs once a day at 06:30 in Lima")
    void limaSchedule() throws Exception {
        var scheduled = AssistantDailySummaryScheduler.class.getMethod("run").getAnnotation(Scheduled.class);
        var properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            properties.load(in);
        }

        assertThat(scheduled.zone()).isEqualTo("America/Lima");
        assertThat(scheduled.cron()).isEqualTo("${viora.assistant.daily-summary.cron:0 30 6 * * *}");
        assertThat(properties.getProperty("viora.assistant.daily-summary.cron"))
                .isEqualTo("${VIORA_ASSISTANT_DAILY_SUMMARY_CRON:0 30 6 * * *}");
        assertThat(properties.getProperty("viora.assistant.daily-summary.enabled"))
                .isEqualTo("${VIORA_ASSISTANT_DAILY_SUMMARY_ENABLED:true}");
    }

    private AssistantDailySummaryScheduler scheduler(int maxProducers) {
        return new AssistantDailySummaryScheduler(orchard, summaries, maxProducers);
    }

    private static Result<AnswerOutcome, ApplicationError> outcome(boolean created) {
        return Result.success(new AnswerOutcome(new AnswerView(mock(AssistantAnswer.class), Freshness.UP_TO_DATE),
                created));
    }
}
