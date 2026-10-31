package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hydric;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.CAMPAIGN;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.MODERATE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.estimated;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.field;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.full;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.harvests;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.onePlot;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.openPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.withAlerts;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.withForecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.withPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.withYields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HomeQuestions: the questions and answers of the home screen, for one plot or the whole field")
class HomeQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final List<QuestionId> QUESTIONS = List.of(QuestionId.HOME_WEEK_PLAN,
            QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, QuestionId.HOME_ALERTS_AFFECTING, QuestionId.HOME_HARVEST_ESTIMATE);

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("HOME is the screen whose questions use the home. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("home")).isEqualTo(ScreenKey.HOME);
        assertThat(ScreenKey.HOME.questionPrefix()).isEqualTo("home.");
        assertThat(QUESTIONS).allSatisfy(question -> assertThat(question.screen()).isEqualTo(ScreenKey.HOME));
        assertThat(onePlot(bare()).screen()).isEqualTo(ScreenKey.HOME);
    }

    @Test
    @DisplayName("facts need today, keep only the heat, frost and hydric alerts, and their version tells scope and plots")
    void facts() {
        assertThatThrownBy(() -> new HomeFacts(null, CAMPAIGN, false, List.of(), THRESHOLDS))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.home.invalid");

        var other = new Incident("SENSOR_OFFLINE", "CRITICAL", 0, 0, "", TODAY, 0, 0);
        assertThat(onePlot(withAlerts(other, heat())).openAlerts()).containsExactly(heat());
        assertThat(new HomeFacts(TODAY, CAMPAIGN, true, null, null).plots()).isEmpty();
        assertThat(onePlot(bare()).canonical())
                .isEqualTo(onePlot(bare()).canonical())
                .isNotEqualTo(field(bare()).canonical())
                .isNotEqualTo(onePlot(withPlan(openPlan(), 0)).canonical())
                .isNotEqualTo(onePlot(withAlerts(heat())).canonical())
                .isNotEqualTo(onePlot(withYields(harvests(2024, 1000), null, null)).canonical())
                .isNotEqualTo(new HomeFacts(TODAY.plusDays(1), CAMPAIGN, false, List.of(bare()), THRESHOLDS)
                        .canonical());
        assertThat(field(bare(), withAlerts(heat())).canonical())
                .isNotEqualTo(field(withAlerts(heat()), bare()).canonical());
    }

    @Test
    @DisplayName("catalog with every figure: the four questions enabled, in order, with their icons")
    void catalogComplete() {
        var questions = QuestionCatalog.questionsFor(onePlot(full(estimated(14250.0), 0, List.of(),
                harvests(2024, 4000, 5000), null)));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly("home.week_plan",
                "home.campaign_vs_previous", "home.alerts_affecting", "home.harvest_estimate");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("calendar", "history", "alert",
                "harvest");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, true);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsOnlyNulls();
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.home.week_plan.label"),
                CatalogText.of("assistant.question.home.campaign_vs_previous.label"),
                CatalogText.of("assistant.question.home.alerts_affecting.label"),
                CatalogText.of("assistant.question.home.harvest_estimate.label"));
    }

    @Test
    @DisplayName("catalog without figures: the week and the alerts enabled, the others with their reason")
    void catalogWithoutData() {
        var questions = QuestionCatalog.questionsFor(onePlot(bare()));

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, false);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(null,
                CatalogText.of("assistant.reason.campaigns_missing"), null,
                CatalogText.of("assistant.reason.caliber_missing"));
        assertThat(answerDraft(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, onePlot(bare()))).isFalse();
        assertThat(answerDraft(QuestionId.HOME_HARVEST_ESTIMATE, onePlot(bare()))).isFalse();
        assertThat(answerDraft(QuestionId.HOME_HARVEST_ESTIMATE, onePlot(withPlan(openPlan(), 0)))).isFalse();
        assertThat(answerDraft(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS,
                onePlot(withYields(harvests(2025, 3000), null, null)))).isFalse();
        assertThat(answerDraft(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS,
                onePlot(withYields(harvests(2024, 0, 3000), null, null)))).isFalse();
    }

    @Test
    @DisplayName("catalog of the field: the alerts speak of my plots; a field without active plots has no week")
    void catalogOfTheField() {
        var questions = QuestionCatalog.questionsFor(field(bare(), bare()));
        assertThat(questions.get(2).label())
                .isEqualTo(CatalogText.of("assistant.question.home.alerts_affecting.label.field"));

        var empty = QuestionCatalog.questionsFor(field());
        assertThat(empty).extracting(CatalogQuestion::enabled).containsExactly(false, false, true, false);
        assertThat(empty.getFirst().disabledReason()).isEqualTo(CatalogText.of("assistant.reason.plots_missing"));
        assertThat(answerDraft(QuestionId.HOME_WEEK_PLAN, field())).isFalse();
        assertThat(localizer.resolve(empty.getFirst().disabledReason(), ES.locale()))
                .isEqualTo("Sin lotes activos · registra un lote");
        assertThat(localizer.resolve(empty.getFirst().disabledReason(), EN.locale()))
                .isEqualTo("No active plots · register a plot");
        assertThat(localizer.resolve(questions.get(2).label(), ES.locale()))
                .isEqualTo("¿Qué alertas afectan a mis lotes?");
        assertThat(localizer.resolve(questions.get(2).label(), EN.locale())).isEqualTo("Which alerts affect my plots?");
    }

    @Test
    @DisplayName("week_plan of a plot: the open window, the hottest day reaching the heat threshold and the alert")
    void weekPlanOpen() {
        var facts = onePlot(full(openPlan(), 0, List.of(heat()), null, hotThenMild()));

        var es = answer(QuestionId.HOME_WEEK_PLAN, facts, ES);
        assertThat(es.text()).isEqualTo("Tu ventana de aclareo está abierta hasta el 10 de noviembre y el plan indica "
                + "quitar el 30 % de los frutos. La máxima de la semana, 35,1 °C el 31 de octubre, alcanza el umbral "
                + "de calor de 32 °C. Tienes 1 alerta activa, la más grave de golpe de calor en nivel aviso.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("35,1 °C", "máxima de la semana", "el 31 oct", FactTone.PRIMARY),
                new AnswerFact("1", "alerta activa", "en este lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el plan de aclareo de esta campaña");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.HOME_WEEK_PLAN, facts, EN);
        assertThat(en.text()).isEqualTo("Your thinning window is open until November 10 and the plan says to remove "
                + "30 % of the fruits. The week maximum, 35.1 °C on October 31, reaches the heat threshold of 32 °C. "
                + "You have 1 open alert, the most severe one of heat wave at warning level.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("35.1 °C", "week maximum", "on Oct 31", FactTone.PRIMARY),
                new AnswerFact("1", "open alert", "on this plot", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the thinning plan of this campaign");
    }

    @Test
    @DisplayName("week_plan of a plot: a window opening this week, a mild week and no alerts")
    void weekPlanOpening() {
        var mild = forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0);
        var facts = onePlot(full(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20)), 0, List.of(), null,
                mild));

        assertThat(answer(QuestionId.HOME_WEEK_PLAN, facts, ES).text()).isEqualTo("Tu ventana de aclareo abre el 2 de "
                + "noviembre y el plan indica quitar el 30 % de los frutos. La máxima de la semana, 31 °C el 31 de "
                + "octubre, queda bajo el umbral de calor de 32 °C. No tienes alertas activas en el lote.");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, facts, EN).text()).isEqualTo("Your thinning window opens on "
                + "November 2 and the plan says to remove 30 % of the fruits. The week maximum, 31 °C on October 31, "
                + "stays below the heat threshold of 32 °C. You have no open alerts on the plot.");

        // The week is today and the next six days: a window opening on 6 November is not this week
        var later = onePlot(withPlan(plan(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 20)), 0));
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, later, ES).text()).startsWith("Esta semana tu lote no tiene "
                + "ventana de aclareo abierta ni muestreo a medias.");
        var lastDay = onePlot(withPlan(plan(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 20)), 0));
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, lastDay, ES).text()).startsWith("Tu ventana de aclareo abre el "
                + "5 de noviembre");
    }

    @Test
    @DisplayName("week_plan of a plot: a sampling left halfway, no forecast and nothing else pending")
    void weekPlanSampling() {
        var facts = onePlot(withPlan(halfway(), 4));

        var es = answer(QuestionId.HOME_WEEK_PLAN, facts, ES);
        assertThat(es.text()).isEqualTo("Al muestreo de tu lote le faltan 4 árboles para calcular el plan de aclareo. "
                + "No hay pronóstico disponible para esta semana. No tienes alertas activas en el lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("Sin pronóstico", "máxima de la semana", "sin datos", FactTone.PRIMARY),
                new AnswerFact("0", "alertas activas", "en este lote", FactTone.HIGHLIGHT));
        var en = answer(QuestionId.HOME_WEEK_PLAN, facts, EN);
        assertThat(en.text()).isEqualTo("The sampling of your plot is missing 4 trees to calculate the thinning plan. "
                + "There is no forecast available for this week. You have no open alerts on the plot.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("No forecast", "week maximum", "no data", FactTone.PRIMARY),
                new AnswerFact("0", "open alerts", "on this plot", FactTone.HIGHLIGHT));

        var nothing = onePlot(bare());
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, nothing, ES).text()).startsWith("Esta semana tu lote no tiene "
                + "ventana de aclareo abierta ni muestreo a medias.");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, nothing, ES).sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas del lote");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, onePlot(withForecast(hotThenMild())), EN).sources())
                .extracting(source -> source.label()).containsExactly("Open-Meteo forecast, updated on October 30");
    }

    @Test
    @DisplayName("week_plan of the field: the first window to close, the hottest day of any plot and the alerts")
    void weekPlanField() {
        var facts = field(withPlan(openPlan(), 0),
                withPlan(plan(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 5)), 0), withPlan(halfway(), 2),
                full(null, 0, List.of(heat(), frost()), null, hotThenMild()), withAlerts(hydric()));

        var es = answer(QuestionId.HOME_WEEK_PLAN, facts, ES);
        assertThat(es.text()).isEqualTo("En 2 de tus 5 lotes la ventana de aclareo está abierta y la primera cierra el "
                + "5 de noviembre. La máxima de la semana, 35,1 °C el 31 de octubre, alcanza el umbral de calor de "
                + "32 °C. Tienes 3 alertas activas en 2 lotes, la más grave de helada en nivel crítico.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("3", "alertas activas", "en 2 lotes",
                FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en tus planes de aclareo de esta campaña");
        var en = answer(QuestionId.HOME_WEEK_PLAN, facts, EN);
        assertThat(en.text()).isEqualTo("On 2 of your 5 plots the thinning window is open and the first one closes "
                + "on November 5. The week maximum, 35.1 °C on October 31, reaches the heat threshold of 32 °C. You "
                + "have 3 open alerts on 2 plots, the most severe one of frost at critical level.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("3", "open alerts", "on 2 plots", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("week_plan of the field: windows opening, samplings halfway, nothing pending")
    void weekPlanFieldVariants() {
        var opening = plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20));
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, field(withPlan(opening, 0),
                withPlan(plan(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 20)), 0), bare()), ES).text())
                .startsWith("En 2 de tus 3 lotes la ventana de aclareo abre esta semana, la primera el 2 de noviembre.");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, field(withPlan(opening, 0), bare()), EN).text())
                .startsWith("On 1 of your 2 plots the thinning window opens this week, on November 2.");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, field(withPlan(halfway(), 2), bare()), ES).text())
                .startsWith("En 1 de tus 2 lotes el muestreo quedó a medias: complétalo para calcular su plan de "
                        + "aclareo.");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, field(withPlan(halfway(), 2), bare()), EN).text())
                .startsWith("On 1 of your 2 plots the sampling was left halfway: complete it to calculate their "
                        + "thinning plan.");

        var nothing = field(bare(), bare());
        var es = answer(QuestionId.HOME_WEEK_PLAN, nothing, ES);
        assertThat(es.text()).isEqualTo("Esta semana ninguno de tus 2 lotes tiene ventana de aclareo abierta ni "
                + "muestreo a medias. No hay pronóstico disponible para esta semana. No tienes alertas activas en "
                + "tus lotes.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("0", "alertas activas", "en ningún lote",
                FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas de tus lotes");
        assertThat(answer(QuestionId.HOME_WEEK_PLAN, nothing, EN).text()).isEqualTo("This week none of your 2 plots "
                + "has an open thinning window or a sampling left halfway. There is no forecast available for this "
                + "week. You have no open alerts on your plots.");
    }

    @Test
    @DisplayName("campaign_vs_previous of a plot: above with the bearing index, below without it, and equal")
    void campaignOfAPlot() {
        var above = onePlot(withYields(harvests(2024, 4000, 5000), 0.5123, MODERATE));

        var es = answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, above, ES);
        assertThat(es.text()).isEqualTo("Tu campaña 2025 cerró con 5.000 kg, un 25 % más que la de 2024 (4.000 kg). "
                + "Tu índice de vecería es 0,51: vecería moderada.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5.000 kg", "cosecha 2025", "2024: 4.000 kg", FactTone.PRIMARY),
                new AnswerFact("+25 %", "frente a la campaña anterior", "2 campañas", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 2 campañas registradas");
        var en = answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, above, EN);
        assertThat(en.text()).isEqualTo("Your 2025 campaign closed with 5,000 kg, 25 % more than the 2024 one "
                + "(4,000 kg). Your alternate bearing index is 0.51: moderate alternate bearing.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("5,000 kg", "harvest 2025", "2024: 4,000 kg", FactTone.PRIMARY),
                new AnswerFact("+25%", "against the previous campaign", "2 campaigns", FactTone.HIGHLIGHT));

        var below = onePlot(withYields(harvests(2024, 5000, 4000), null, null));
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, below, ES).text()).isEqualTo("Tu campaña 2025 cerró "
                + "con 4.000 kg, un 20 % menos que la de 2024 (5.000 kg). Aún no hay índice de vecería para leer la "
                + "tendencia.");
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, below, ES).facts().get(1).value()).isEqualTo("−20 %");
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, below, EN).text()).isEqualTo("Your 2025 campaign "
                + "closed with 4,000 kg, 20 % less than the 2024 one (5,000 kg). There is no alternate bearing index "
                + "yet to read the trend.");

        var equal = onePlot(withYields(harvests(2023, 3000, 5000, 5000), 0.5123, "UNCLASSIFIED"));
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, equal, ES).text()).isEqualTo("Tu campaña 2025 cerró "
                + "con 5.000 kg, en línea con la de 2024 (5.000 kg). Aún no hay índice de vecería para leer la "
                + "tendencia.");
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, equal, EN).facts()).containsExactly(
                new AnswerFact("5,000 kg", "harvest 2025", "2024: 5,000 kg", FactTone.PRIMARY),
                new AnswerFact("0%", "against the previous campaign", "3 campaigns", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("campaign_vs_previous of the field: only the plots with both harvests are summed")
    void campaignOfTheField() {
        var facts = field(withYields(harvests(2024, 1000, 3000), null, null),
                withYields(harvests(2025, 2000), null, null), withYields(harvests(2023, 4000, 4000, 4000), null, null));

        var es = answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, facts, ES);
        assertThat(es.text()).isEqualTo("En los 2 lotes con las dos cosechas, la campaña 2025 cerró con 7.000 kg, un "
                + "40 % más que la de 2024 (5.000 kg). Queda fuera 1 lote sin las dos cosechas.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("7.000 kg", "cosecha 2025", "2024: 5.000 kg", FactTone.PRIMARY),
                new AnswerFact("+40 %", "frente a la campaña anterior", "2 lotes comparados", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las cosechas registradas de 2 lotes");
        var en = answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, facts, EN);
        assertThat(en.text()).isEqualTo("On the 2 plots with both harvests, the 2025 campaign closed with 7,000 kg, "
                + "40 % more than the 2024 one (5,000 kg). 1 plot without both harvests is left out.");
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the recorded harvests of 2 plots");

        var all = field(withYields(harvests(2024, 5000, 4000), null, null),
                withYields(harvests(2024, 1000, 500), null, null));
        assertThat(answer(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, all, ES).text()).isEqualTo("En los 2 lotes con las "
                + "dos cosechas, la campaña 2025 cerró con 4.500 kg, un 25 % menos que la de 2024 (6.000 kg). Entran "
                + "todos tus lotes activos.");
        // No plot has both of the two latest harvests: nothing to compare
        assertThat(answerDraft(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, field(
                withYields(harvests(2024, 1000), null, null), withYields(harvests(2025, 2000), null, null))))
                .isFalse();
    }

    @Test
    @DisplayName("alerts_affecting of a plot: none, or the most severe one against its threshold")
    void alertsOfAPlot() {
        var none = answer(QuestionId.HOME_ALERTS_AFFECTING, onePlot(bare()), ES);
        assertThat(none.text()).isEqualTo("Tu lote no tiene alertas activas de calor, helada ni estrés hídrico. Revisa "
                + "el pronóstico de la semana en la pantalla de clima.");
        assertThat(none.facts()).containsExactly(
                new AnswerFact("0", "alertas activas", "calor, helada o estrés hídrico", FactTone.PRIMARY),
                new AnswerFact("Sin alertas", "estado del lote", "en este lote", FactTone.HIGHLIGHT));
        assertThat(none.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas del lote");
        assertThat(answer(QuestionId.HOME_ALERTS_AFFECTING, onePlot(bare()), EN).text()).isEqualTo("Your plot has no "
                + "open heat, frost or water stress alerts. Check the forecast of the week on the climate screen.");

        var heat = onePlot(withAlerts(heat()));
        var es = answer(QuestionId.HOME_ALERTS_AFFECTING, heat, ES);
        assertThat(es.text()).isEqualTo("Tu lote tiene 1 alerta activa y la más grave es de golpe de calor en nivel "
                + "aviso: 34,2 °C frente al umbral de 32 °C. Ábrela para seguir sus pasos de mitigación.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("34,2 °C", "máxima prevista", "umbral 32 °C", FactTone.PRIMARY),
                new AnswerFact("1", "alerta activa", "desde el 28 oct", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en la alerta del 28 de octubre");
        var en = answer(QuestionId.HOME_ALERTS_AFFECTING, heat, EN);
        assertThat(en.text()).isEqualTo("Your plot has 1 open alert and the most severe one is of heat wave at "
                + "warning level: 34.2 °C against the threshold of 32 °C. Open it to follow its mitigation steps.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("1", "open alert", "since Oct 28", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("alerts_affecting of the field: none in any plot, or the most severe one of every plot")
    void alertsOfTheField() {
        var none = answer(QuestionId.HOME_ALERTS_AFFECTING, field(bare(), bare()), ES);
        assertThat(none.text()).isEqualTo("Tus lotes no tienen alertas activas de calor, helada ni estrés hídrico. "
                + "Revisa el pronóstico de la semana en la pantalla de clima.");
        assertThat(none.facts().get(1)).isEqualTo(new AnswerFact("2", "lotes activos", "solo tus lotes",
                FactTone.HIGHLIGHT));
        assertThat(none.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas de tus lotes");
        assertThat(answer(QuestionId.HOME_ALERTS_AFFECTING, field(bare(), bare()), EN).text()).isEqualTo("Your plots "
                + "have no open heat, frost or water stress alerts. Check the forecast of the week on the climate "
                + "screen.");

        var facts = field(withAlerts(heat()), withAlerts(frost()), bare());
        var es = answer(QuestionId.HOME_ALERTS_AFFECTING, facts, ES);
        assertThat(es.text()).isEqualTo("Tienes 2 alertas activas en 2 de tus 3 lotes y la más grave es de helada en "
                + "nivel crítico: -2,4 °C frente al umbral de -2 °C. Ábrela para seguir sus pasos de mitigación.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("2", "alertas activas", "en 2 lotes",
                FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas de tus lotes");
        assertThat(answer(QuestionId.HOME_ALERTS_AFFECTING, facts, EN).text()).isEqualTo("You have 2 open alerts on 2 "
                + "of your 3 plots and the most severe one is of frost at critical level: -2.4 °C against the "
                + "threshold of -2 °C. Open it to follow its mitigation steps.");

        // A field of one plot reads as that plot
        assertThat(answer(QuestionId.HOME_ALERTS_AFFECTING, field(withAlerts(heat())), ES).text())
                .startsWith("Tu lote tiene 1 alerta activa");
    }

    @Test
    @DisplayName("harvest_estimate of a plot: the yield and caliber after the thinning, or the caliber alone")
    void harvestOfAPlot() {
        var facts = onePlot(withPlan(estimated(14250.0), 0));

        var es = answer(QuestionId.HOME_HARVEST_ESTIMATE, facts, ES);
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 14.250 kg por hectárea para "
                + "esta campaña, con un calibre más probable de 180 frutos por kilo (171/190). Es una proyección en "
                + "calibración.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("14.250", "kg por hectárea estimados", "en calibración", FactTone.PRIMARY),
                new AnswerFact("180", "frutos por kilo · calibre más probable", "calibre 171/190", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el aclareo registrado el 28 de octubre");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, facts, EN).text()).isEqualTo("With the load your thinning "
                + "left, Viora estimates 14,250 kg per hectare for this campaign, with a most likely caliber of 180 "
                + "fruits per kilo (171/190). It is a projection still being calibrated.");

        var noYield = onePlot(withPlan(estimated(null), 0));
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, noYield, ES).text()).isEqualTo("Tras tu aclareo, el "
                + "calibre más probable es de 180 frutos por kilo (171/190). Viora aún no estima los kilos de la "
                + "cosecha: faltan cosechas registradas o la superficie del lote.");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, noYield, EN).text()).isEqualTo("After your thinning, the "
                + "most likely caliber is 180 fruits per kilo (171/190). Viora does not estimate the harvest kilos "
                + "yet: recorded harvests or the plot area are missing.");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, noYield, EN).facts().getFirst()).isEqualTo(
                new AnswerFact("Not estimated", "harvest kilos", "data missing", FactTone.PRIMARY));

        var unconfirmed = new ThinningPlanData(true, 30.0, 0.5714, 0.4, null, null, 5, TODAY, null, "ESTIMATED",
                180.4, "171/190", 14250.0);
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, onePlot(full(unconfirmed, 0, List.of(),
                harvests(2024, 4000, 5000), null)), ES).sources()).extracting(source -> source.label())
                .containsExactly("Basado en 2 campañas registradas");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, onePlot(withPlan(unconfirmed, 0)), ES).sources())
                .extracting(source -> source.label()).containsExactly("Basado en el plan de aclareo de esta campaña");
    }

    @Test
    @DisplayName("harvest_estimate of the field: the range of the estimated plots and how many still miss it")
    void harvestOfTheField() {
        var range = field(withPlan(estimated(8213.6), 0), withPlan(estimated(9100.2), 0), bare());
        var es = answer(QuestionId.HOME_HARVEST_ESTIMATE, range, ES);
        assertThat(es.text()).isEqualTo("En 2 de tus 3 lotes, Viora estima entre 8.214 y 9.100 kg por hectárea tras "
                + "el aclareo. A 1 lote aún le falta la estimación.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("8.214–9.100", "kg por hectárea estimados", "en calibración", FactTone.PRIMARY),
                new AnswerFact("2", "lotes con estimación", "de 3 lotes activos", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en los aclareos registrados de esta campaña");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, range, EN).text()).isEqualTo("On 2 of your 3 plots, Viora "
                + "estimates between 8,214 and 9,100 kg per hectare after thinning. 1 plot has no estimate yet.");

        var single = field(withPlan(estimated(8000.0), 0), withPlan(estimated(8000.4), 0));
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, single, ES).text()).isEqualTo("En 2 de tus 2 lotes, Viora "
                + "estima 8.000 kg por hectárea tras el aclareo. Todos tus lotes activos tienen estimación.");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, single, EN).text()).isEqualTo("On 2 of your 2 plots, Viora "
                + "estimates 8,000 kg per hectare after thinning. All your active plots have an estimate.");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, single, EN).facts().getFirst().value()).isEqualTo("8,000");

        var noYield = field(withPlan(estimated(null), 0), bare());
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, noYield, ES).text()).isEqualTo("1 de tus 2 lotes ya tiene "
                + "calibre estimado tras el aclareo, pero Viora aún no estima sus kilos. Faltan cosechas registradas o "
                + "la superficie de los lotes.");
        assertThat(answer(QuestionId.HOME_HARVEST_ESTIMATE, noYield, EN).text()).isEqualTo("1 of your 2 plots already "
                + "has an estimated caliber after thinning, but Viora does not estimate their kilos yet. Recorded "
                + "harvests or the plot areas are missing.");
    }

    @Test
    @DisplayName("every variant resolves in es and en, has two cards and passes the validator as its own rephrasing")
    void everyTemplatePassesAsItsOwnRephrasing() {
        var variants = new HashSet<String>();
        int rendered = 0;
        for (var facts : scenarios()) {
            for (var question : QUESTIONS) {
                var draft = AnswerComposer.compose(question, facts, PLOT);
                if (draft.isEmpty()) {
                    continue;
                }
                collectVariants(draft.get().text(), variants);
                for (var language : List.of(ES, EN)) {
                    var answer = localizer.localize(draft.get(), language);
                    var where = draft.get().text().key() + " [" + language.code() + "]: " + answer.text();
                    var texts = new ArrayList<>(List.of(answer.text()));
                    answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                    answer.sources().forEach(source -> texts.add(source.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).as(where).hasSize(2);
                    assertThat(AnswerTextValidator.validate(answer.text(), answer.text(), answer.facts(), language))
                            .as(where).isEqualTo(Verdict.ACCEPTED);
                    rendered++;
                }
            }
        }
        // Week plan: 1 text and 15 sentences; alerts: 4; campaign: 6 texts and 3 closings; harvest: 5
        assertThat(variants).hasSize(34);
        assertThat(rendered).isGreaterThan(60);
    }

    @Test
    @DisplayName("a question of another screen is not answered by the home screen")
    void otherScreenQuestion() {
        assertThat(HomeQuestions.answer(QuestionId.PLOT_TODAY, onePlot(bare()))).isEmpty();
    }

    private static List<HomeFacts> scenarios() {
        var opening = plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20));
        var closesFirst = plan(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 5));
        var mild = forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0);
        return List.of(
                onePlot(full(openPlan(), 0, List.of(heat()), harvests(2023, 9600, 3100, 8300), hotThenMild())),
                onePlot(full(opening, 0, List.of(heat(), hydric()), harvests(2024, 5000, 4000), mild)),
                onePlot(full(halfway(), 1, List.of(), harvests(2024, 5000, 5000), null)),
                onePlot(full(halfway(), 4, List.of(), null, null)),
                onePlot(withYields(harvests(2024, 4000, 5000), null, null)),
                onePlot(withPlan(estimated(8213.6), 0)),
                onePlot(withPlan(estimated(null), 0)),
                field(withPlan(openPlan(), 0), withPlan(closesFirst, 0), withPlan(halfway(), 2),
                        full(null, 0, List.of(heat(), frost()), harvests(2024, 1000, 3000), hotThenMild()),
                        withAlerts(hydric())),
                field(withPlan(openPlan(), 0), bare()),
                field(withPlan(opening, 0), withPlan(opening, 0), withForecast(mild)),
                field(withPlan(opening, 0), bare()),
                field(withPlan(halfway(), 2), withPlan(halfway(), 1), withAlerts(heat())),
                field(withPlan(halfway(), 2), bare()),
                field(bare(), bare(), withAlerts(heat())),
                field(withYields(harvests(2024, 1000, 3000), null, null), withYields(harvests(2025, 2000), null, null),
                        withYields(harvests(2023, 4000, 4000, 4000), null, null)),
                field(withYields(harvests(2024, 5000, 4000), null, null),
                        withYields(harvests(2024, 1000, 500), null, null)),
                field(withYields(harvests(2024, 3000, 3000), null, null),
                        withYields(harvests(2024, 2000, 2000), null, null)),
                field(withPlan(estimated(8213.6), 0), withPlan(estimated(9100.2), 0), bare()),
                field(withPlan(estimated(8000.0), 0), withPlan(estimated(8000.4), 0)),
                field(withPlan(estimated(8000.0), 0), withPlan(estimated(null), 0), bare()),
                field(withPlan(estimated(null), 0), bare()),
                field(withPlan(estimated(null), 0), withPlan(estimated(null), 0), bare()),
                field());
    }

    /** The text key and every nested sentence key of the home screen. */
    private static void collectVariants(CatalogText text, Set<String> variants) {
        variants.add(text.key());
        text.args().forEach(arg -> {
            if (arg instanceof CatalogText nested && nested.key().startsWith("assistant.answer.home.")) {
                collectVariants(nested, variants);
            }
        });
    }

    private boolean answerDraft(QuestionId question, HomeFacts facts) {
        return AnswerComposer.compose(question, facts, PLOT).isPresent();
    }

    private AnswerLocalizer.Localized answer(QuestionId question, HomeFacts facts, AnswerLanguage language) {
        var draft = AnswerComposer.compose(question, facts, PLOT);
        assertThat(draft).as(question.value()).isPresent();
        assertThat(draft.get().facts()).hasSize(2);
        return localizer.localize(draft.get(), language);
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
