package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedSamples.closed;
import static com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedSamples.contractHarvests;
import static com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedSamples.harvests;
import static com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedSamples.harvestsOnly;
import static com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedSamples.outcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CampaignClosedQuestions: the questions and answers of the closed campaign screen")
class CampaignClosedQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final List<QuestionId> QUESTIONS = List.of(QuestionId.CAMPAIGN_VS_PREVIOUS,
            QuestionId.CAMPAIGN_CALIBER_CHANGE, QuestionId.CAMPAIGN_PLAN_NEXT);
    private static final CatalogText CALIBER_REASON =
            CatalogText.of("assistant.reason.caliber_history_unavailable");

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("CAMPAIGN_CLOSED is a plot screen whose questions use the contract prefix campaign.")
    void screen() {
        assertThat(ScreenKey.fromValue("campaign_closed")).isEqualTo(ScreenKey.CAMPAIGN_CLOSED);
        assertThat(ScreenKey.CAMPAIGN_CLOSED.requiresPlot()).isTrue();
        assertThat(ScreenKey.CAMPAIGN_CLOSED.questionPrefix()).isEqualTo("campaign.");
        assertThat(QuestionId.CAMPAIGN_VS_PREVIOUS.value()).isEqualTo("campaign.vs_previous");
        assertThat(QuestionId.CAMPAIGN_CALIBER_CHANGE.value()).isEqualTo("campaign.caliber_change");
        assertThat(QuestionId.CAMPAIGN_PLAN_NEXT.value()).isEqualTo("campaign.plan_next");
        assertThat(new QuestionId("campaign.plan_next").screen()).isEqualTo(ScreenKey.CAMPAIGN_CLOSED);
        assertThatThrownBy(() -> new QuestionId("campaign_closed.plan_next").screen())
                .hasMessage("assistant.screen.invalid");
    }

    @Test
    @DisplayName("catalog with history: three questions in contract order, labels name the closed and next years, "
            + "caliber_change always disabled")
    void catalogWithHistory() {
        var facts = closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5);
        var questions = QuestionCatalog.questionsFor(facts);

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "campaign.vs_previous", "campaign.caliber_change", "campaign.plan_next");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("history", "fruit", "calendar");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(null, CALIBER_REASON,
                null);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.campaign.vs_previous.label", "2025"),
                CatalogText.of("assistant.question.campaign.caliber_change.label"),
                CatalogText.of("assistant.question.campaign.plan_next.label", "2026"));
        assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.forLanguageTag("es")))
                .containsExactly("¿Cómo fue la campaña 2025 frente a las anteriores?",
                        "¿Cambió el calibre frente a otras campañas?", "¿Qué planifico para la campaña 2026?");
        assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.ENGLISH))
                .containsExactly("How did the 2025 campaign go against the earlier ones?",
                        "Did the caliber change against other campaigns?", "What do I plan for the 2026 campaign?");
        assertThat(localizer.resolve(questions.get(1).disabledReason(), Locale.forLanguageTag("es")))
                .isEqualTo("El calibre de cada campaña aún no llega al asistente");
        assertThat(localizer.resolve(questions.get(1).disabledReason(), Locale.ENGLISH))
                .isEqualTo("The caliber of each campaign does not reach the assistant yet");
    }

    @Test
    @DisplayName("catalog with one harvest: the comparison waits for 2 campaigns, the plan is offered")
    void catalogWithOneHarvest() {
        var facts = harvestsOnly(harvests(2025, 9000.0));
        var questions = QuestionCatalog.questionsFor(facts);

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, false, true);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.campaigns_missing"), CALIBER_REASON, null);
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.campaign.vs_previous.label", "2025"));
        assertThat(localizer.resolve(questions.get(0).disabledReason(), Locale.forLanguageTag("es")))
                .isEqualTo("Registra al menos 2 campañas");
        assertThat(localizer.resolve(questions.get(0).disabledReason(), Locale.ENGLISH))
                .isEqualTo("Record at least 2 campaigns");
        assertThat(AnswerComposer.compose(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.CAMPAIGN_PLAN_NEXT, facts, PLOT)).isPresent();
    }

    @Test
    @DisplayName("catalog without a harvest: labels without year, every question disabled and nothing answered")
    void catalogWithoutHarvest() {
        for (var facts : List.of(harvestsOnly(new TreeMap<>()), harvestsOnly(null),
                closed(null, 0.4, "REGULAR", outcome("OPTIMAL", 14, 49), 1.5))) {
            var questions = QuestionCatalog.questionsFor(facts);

            assertThat(facts.closedCampaignYear()).isEmpty();
            assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                    "campaign.vs_previous", "campaign.caliber_change", "campaign.plan_next");
            assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("history", "fruit", "calendar");
            assertThat(questions).noneMatch(CatalogQuestion::enabled);
            assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                    CatalogText.of("assistant.reason.harvest_missing"), CALIBER_REASON,
                    CatalogText.of("assistant.reason.harvest_missing"));
            assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.forLanguageTag("es")))
                    .containsExactly("¿Cómo fue mi campaña frente a las anteriores?",
                            "¿Cambió el calibre frente a otras campañas?", "¿Qué planifico para la próxima campaña?");
            assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.ENGLISH))
                    .containsExactly("How did my campaign go against the earlier ones?",
                            "Did the caliber change against other campaigns?", "What do I plan for the next campaign?");
            assertThat(localizer.resolve(questions.get(0).disabledReason(), Locale.forLanguageTag("es")))
                    .isEqualTo("Sin cosecha registrada · registra la cosecha de la campaña");
            assertThat(localizer.resolve(questions.get(2).disabledReason(), Locale.ENGLISH))
                    .isEqualTo("No harvest recorded · record the harvest of the campaign");
            for (var question : QUESTIONS) {
                assertThat(AnswerComposer.compose(question, facts, PLOT)).as(question.value()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("caliber_change is never answered, and a question of another screen is not either")
    void caliberChangeNeverAnswered() {
        var facts = closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5);

        assertThat(AnswerComposer.compose(QuestionId.CAMPAIGN_CALIBER_CHANGE, facts, PLOT)).isEmpty();
        assertThat(CampaignClosedQuestions.answer(QuestionId.CHILL_MISSING, facts)).isEmpty();
    }

    @Test
    @DisplayName("vs_previous above in t/ha: 8,3 t/ha, 36 % over the average of 3 campaigns, in es and en")
    void versusPreviousAboveInTons() {
        var facts = closed(contractHarvests(), null, null, null, 1.5);

        var es = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu campaña 2025 cerró con 8,3 t/ha (12.450 kg), un 36 % más que el promedio "
                + "de tus 3 campañas anteriores (6,1 t/ha).");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("8,3 t/ha", "cosecha 2025", "promedio 6,1 t/ha", FactTone.PRIMARY),
                new AnswerFact("+36 %", "frente al promedio", "3 campañas anteriores", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Superficie del lote según su polígono: 1,50 ha");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "en");
        assertThat(en.text()).isEqualTo("Your 2025 campaign closed at 8.3 t/ha (12,450 kg), 36% above the average of "
                + "your 3 previous campaigns (6.1 t/ha).");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("8.3 t/ha", "harvest 2025", "average 6.1 t/ha", FactTone.PRIMARY),
                new AnswerFact("+36%", "against the average", "3 previous campaigns", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Plot area from its polygon: 1.50 ha");
    }

    @Test
    @DisplayName("vs_previous below in t/ha against the previous campaign, in es and en")
    void versusPreviousBelowInTons() {
        var facts = closed(harvests(2024, 10000.0, 7000.0), null, null, null, 2.0);

        var es = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu campaña 2025 cerró con 3,5 t/ha (7.000 kg), un 30 % menos que tu campaña "
                + "anterior (5,0 t/ha).");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("3,5 t/ha", "cosecha 2025", "promedio 5,0 t/ha", FactTone.PRIMARY),
                new AnswerFact("−30 %", "frente al promedio", "1 campaña anterior", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Superficie del lote según su polígono: 2,00 ha");

        var en = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "en");
        assertThat(en.text()).isEqualTo("Your 2025 campaign closed at 3.5 t/ha (7,000 kg), 30% below your previous "
                + "campaign (5.0 t/ha).");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("3.5 t/ha", "harvest 2025", "average 5.0 t/ha", FactTone.PRIMARY),
                new AnswerFact("−30%", "against the average", "1 previous campaign", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("vs_previous equal in t/ha: a difference that rounds to 0 % reads as in line, in es and en")
    void versusPreviousEqualInTons() {
        // 10 040 kg against 10 000 kg is 0.4 % above: rounded, in line with the previous campaign
        var facts = closed(harvests(2024, 10000.0, 10040.0), null, null, null, 2.0);

        var es = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu campaña 2025 cerró con 5,0 t/ha (10.040 kg), en línea con tu campaña "
                + "anterior (5,0 t/ha).");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5,0 t/ha", "cosecha 2025", "promedio 5,0 t/ha", FactTone.PRIMARY),
                new AnswerFact("0 %", "frente al promedio", "1 campaña anterior", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "en");
        assertThat(en.text()).isEqualTo("Your 2025 campaign closed at 5.0 t/ha (10,040 kg), in line with your previous "
                + "campaign (5.0 t/ha).");
        assertThat(en.facts().get(1)).isEqualTo(
                new AnswerFact("0%", "against the average", "1 previous campaign", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("vs_previous in kilograms without the plot area (or with an area of 0): above, in es and en")
    void versusPreviousAboveInKilograms() {
        for (Double area : new Double[]{null, 0.0}) {
            var facts = closed(contractHarvests(), null, null, null, area);

            var es = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "es");
            assertThat(es.text()).isEqualTo("Tu campaña 2025 cerró con 12.450 kg, un 36 % más que el promedio de tus 3 "
                    + "campañas anteriores (9.150 kg).");
            assertThat(es.facts()).containsExactly(
                    new AnswerFact("12.450 kg", "cosecha 2025", "promedio 9.150 kg", FactTone.PRIMARY),
                    new AnswerFact("+36 %", "frente al promedio", "3 campañas anteriores", FactTone.HIGHLIGHT));
            assertThat(es.sources()).extracting(source -> source.label())
                    .containsExactly("Basado en 4 campañas registradas");

            var en = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "en");
            assertThat(en.text()).isEqualTo("Your 2025 campaign closed at 12,450 kg, 36% above the average of your 3 "
                    + "previous campaigns (9,150 kg).");
            assertThat(en.facts()).containsExactly(
                    new AnswerFact("12,450 kg", "harvest 2025", "average 9,150 kg", FactTone.PRIMARY),
                    new AnswerFact("+36%", "against the average", "3 previous campaigns", FactTone.HIGHLIGHT));
            assertThat(en.sources()).extracting(source -> source.label())
                    .containsExactly("Based on 4 recorded campaigns");
        }
    }

    @Test
    @DisplayName("vs_previous in kilograms: below and equal, in es and en")
    void versusPreviousBelowAndEqualInKilograms() {
        var below = harvestsOnly(harvests(2024, 10000.0, 7000.0));
        var esBelow = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, below, "es");
        assertThat(esBelow.text()).isEqualTo("Tu campaña 2025 cerró con 7.000 kg, un 30 % menos que tu campaña "
                + "anterior (10.000 kg).");
        assertThat(esBelow.facts()).containsExactly(
                new AnswerFact("7.000 kg", "cosecha 2025", "promedio 10.000 kg", FactTone.PRIMARY),
                new AnswerFact("−30 %", "frente al promedio", "1 campaña anterior", FactTone.HIGHLIGHT));
        assertThat(esBelow.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 2 campañas registradas");
        var enBelow = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, below, "en");
        assertThat(enBelow.text()).isEqualTo("Your 2025 campaign closed at 7,000 kg, 30% below your previous campaign "
                + "(10,000 kg).");
        assertThat(enBelow.facts().get(1)).isEqualTo(
                new AnswerFact("−30%", "against the average", "1 previous campaign", FactTone.HIGHLIGHT));
        assertThat(enBelow.sources()).extracting(source -> source.label())
                .containsExactly("Based on 2 recorded campaigns");

        var equal = harvestsOnly(harvests(2023, 9000.0, 9000.0, 9000.0));
        var esEqual = answer(QuestionId.CAMPAIGN_VS_PREVIOUS, equal, "es");
        assertThat(esEqual.text()).isEqualTo("Tu campaña 2025 cerró con 9.000 kg, en línea con el promedio de tus 2 "
                + "campañas anteriores (9.000 kg).");
        assertThat(esEqual.facts()).containsExactly(
                new AnswerFact("9.000 kg", "cosecha 2025", "promedio 9.000 kg", FactTone.PRIMARY),
                new AnswerFact("0 %", "frente al promedio", "2 campañas anteriores", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.CAMPAIGN_VS_PREVIOUS, equal, "en").text()).isEqualTo("Your 2025 campaign closed "
                + "at 9,000 kg, in line with the average of your 2 previous campaigns (9,000 kg).");
    }

    @Test
    @DisplayName("vs_previous: the percentage comes from the kilograms, never from the rounded tonnes")
    void percentageFromKilograms() {
        // 6.04 and 6.66 t/ha round to 6.0 and 6.7 (11.7 %), but the kilograms give 10.3 %, shown as 10 %
        var facts = closed(harvests(2024, 6040.0, 6660.0), null, null, null, 1.0);

        assertThat(answer(QuestionId.CAMPAIGN_VS_PREVIOUS, facts, "es").text()).isEqualTo("Tu campaña 2025 cerró con "
                + "6,7 t/ha (6.660 kg), un 10 % más que tu campaña anterior (6,0 t/ha).");
    }

    @Test
    @DisplayName("plan_next with an alternating band, an on-time thinning and the window of the profile, in es and en")
    void planNextAlternatingOnTime() {
        var facts = closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5);

        var es = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,51 (vecería moderada): la herramienta del plan es "
                + "el aclareo según la carga de frutos. En la campaña 2025 registraste el aclareo el 3 de noviembre, "
                + "dentro de la ventana. Para la campaña 2026, registra la plena floración de tu lote, muestrea tus "
                + "árboles y aclarea dentro de la ventana, que en el perfil de tu variedad va de 14 a 49 días después "
                + "de la floración.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("2026", "próximo muestreo", "tras la plena floración", FactTone.PRIMARY),
                new AnswerFact("0,51", "índice de vecería", "vecería moderada", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 4 campañas registradas");
        assertThat(es.actions()).isEmpty();
        // It never projects the next harvest
        assertThat(es.text()).doesNotContain("kg").doesNotContain("t/ha");

        var en = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "en");
        assertThat(en.text()).isEqualTo("Your alternate bearing index is 0.51 (moderate alternate bearing): the tool "
                + "of the plan is thinning according to the fruit load. In the 2025 campaign you recorded the thinning "
                + "on November 3, within the window. For the 2026 campaign, record the full bloom of your plot, "
                + "sample your trees and thin within the window, which in the profile of your variety runs from 14 to "
                + "49 days after bloom.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("2026", "next sampling", "after full bloom", FactTone.PRIMARY),
                new AnswerFact("0.51", "alternate bearing index", "moderate alternate bearing", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 4 recorded campaigns");
        assertThat(en.text()).doesNotContain("kg").doesNotContain("t/ha");
    }

    @Test
    @DisplayName("plan_next with a severe band also points to thinning by load")
    void planNextSevere() {
        var facts = closed(contractHarvests(), 0.8, "SEVERE_ALTERNATION", null, 1.5);

        assertThat(answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "es").text()).startsWith("Tu índice de vecería es "
                + "0,80 (vecería severa): la herramienta del plan es el aclareo según la carga de frutos. En la "
                + "campaña 2025 no registraste aclareo.");
    }

    @Test
    @DisplayName("plan_next with a regular band and a late thinning without window days, in es and en")
    void planNextRegularLate() {
        var facts = closed(harvests(2024, 9000.0, 12450.0), 0.2, "REGULAR", outcome("LATE", null, null), 1.5);

        var es = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,20 (producción regular). En la campaña 2025 "
                + "registraste el aclareo el 3 de noviembre, fuera de la ventana, cuando su efecto sobre la vecería es "
                + "menor. Para la campaña 2026, registra la plena floración de tu lote y muestrea tus árboles para que "
                + "Viora calcule el plan de aclareo.");
        assertThat(es.facts().get(1)).isEqualTo(
                new AnswerFact("0,20", "índice de vecería", "producción regular", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 2 campañas registradas");

        var en = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "en");
        assertThat(en.text()).isEqualTo("Your alternate bearing index is 0.20 (regular bearing). In the 2025 campaign "
                + "you recorded the thinning on November 3, after the window, when its effect on alternate bearing is "
                + "smaller. For the 2026 campaign, record the full bloom of your plot and sample your trees so Viora "
                + "can calculate the thinning plan.");
        assertThat(en.facts().get(1)).isEqualTo(
                new AnswerFact("0.20", "alternate bearing index", "regular bearing", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("plan_next without index nor thinning: the short text and the not-thinned card, in es and en")
    void planNextWithoutIndexNorThinning() {
        var facts = harvestsOnly(harvests(2025, 12450.0));

        var es = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "es");
        assertThat(es.text()).isEqualTo("En la campaña 2025 no registraste aclareo. Para la campaña 2026, registra la "
                + "plena floración de tu lote y muestrea tus árboles para que Viora calcule el plan de aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("2026", "próximo muestreo", "tras la plena floración", FactTone.PRIMARY),
                new AnswerFact("Sin aclareo", "aclareo 2025", "registra un muestreo", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Basado en 1 campaña registrada");

        var en = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "en");
        assertThat(en.text()).isEqualTo("In the 2025 campaign you recorded no thinning. For the 2026 campaign, record "
                + "the full bloom of your plot and sample your trees so Viora can calculate the thinning plan.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("2026", "next sampling", "after full bloom", FactTone.PRIMARY),
                new AnswerFact("No thinning", "thinning 2025", "record a sampling", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 1 recorded campaign");
    }

    @Test
    @DisplayName("plan_next without index and a thinning of unknown timeliness: recorded with its day, in es and en")
    void planNextUndatedTimeliness() {
        for (String timeliness : new String[]{null, "EARLY"}) {
            var facts = closed(harvests(2025, 12450.0), null, null, outcome(timeliness, null, null), null);

            var es = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "es");
            assertThat(es.text()).isEqualTo("En la campaña 2025 registraste el aclareo el 3 de noviembre. Para la "
                    + "campaña 2026, registra la plena floración de tu lote y muestrea tus árboles para que Viora "
                    + "calcule el plan de aclareo.");
            assertThat(es.facts().get(1)).isEqualTo(
                    new AnswerFact("Registrado", "aclareo 2025", "el 3 nov", FactTone.HIGHLIGHT));

            var en = answer(QuestionId.CAMPAIGN_PLAN_NEXT, facts, "en");
            assertThat(en.text()).isEqualTo("In the 2025 campaign you recorded the thinning on November 3. For the "
                    + "2026 campaign, record the full bloom of your plot and sample your trees so Viora can calculate "
                    + "the thinning plan.");
            assertThat(en.facts().get(1)).isEqualTo(
                    new AnswerFact("Recorded", "thinning 2025", "on Nov 3", FactTone.HIGHLIGHT));
        }
    }

    @Test
    @DisplayName("plan_next: an index without a known band, or a window with one end missing, are left out")
    void planNextPartialFigures() {
        var unknownBand = closed(harvests(2024, 9000.0, 12450.0), 0.3, "UNKNOWN", outcome("OPTIMAL", 14, null), 1.5);

        var es = answer(QuestionId.CAMPAIGN_PLAN_NEXT, unknownBand, "es");
        assertThat(es.text()).isEqualTo("En la campaña 2025 registraste el aclareo el 3 de noviembre, dentro de la "
                + "ventana. Para la campaña 2026, registra la plena floración de tu lote y muestrea tus árboles para "
                + "que Viora calcule el plan de aclareo.");
        assertThat(es.facts().get(1)).isEqualTo(
                new AnswerFact("Registrado", "aclareo 2025", "el 3 nov", FactTone.HIGHLIGHT));

        var bandWithoutIndex = closed(harvests(2024, 9000.0, 12450.0), null, "REGULAR", null, 1.5);
        assertThat(answer(QuestionId.CAMPAIGN_PLAN_NEXT, bandWithoutIndex, "es").text())
                .startsWith("En la campaña 2025 no registraste aclareo.");
    }

    @Test
    @DisplayName("the data version follows every figure the answers read")
    void dataVersion() {
        var base = closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5);
        var version = AnswerComposer.dataVersionOf(ScreenKey.CAMPAIGN_CLOSED, base);

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.CAMPAIGN_CLOSED, closed(contractHarvests(), 0.5123,
                "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5))).isEqualTo(version);
        for (var changed : List.of(
                closed(harvests(2022, 8700.0, 9150.0, 9600.0, 12460.0), 0.5123, "MODERATE_ALTERNATION",
                        outcome("OPTIMAL", 14, 49), 1.5),
                closed(contractHarvests(), 0.6, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5),
                closed(contractHarvests(), 0.5123, "SEVERE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.5),
                closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("LATE", 14, 49), 1.5),
                closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", null, 1.5),
                closed(contractHarvests(), 0.5123, "MODERATE_ALTERNATION", outcome("OPTIMAL", 14, 49), 1.6))) {
            assertThat(AnswerComposer.dataVersionOf(ScreenKey.CAMPAIGN_CLOSED, changed)).isNotEqualTo(version);
        }
    }

    private AnswerLocalizer.Localized answer(QuestionId question, CampaignClosedFacts facts, String language) {
        var draft = AnswerComposer.compose(question, facts, PLOT);
        assertThat(draft).as(question.value()).isPresent();
        assertThat(draft.get().facts()).hasSize(2);
        return localizer.localize(draft.get(), new AnswerLanguage(language));
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
