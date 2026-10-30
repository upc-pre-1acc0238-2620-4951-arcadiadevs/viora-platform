package com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.QuestionResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog labels localized by Accept-Language")
class QuestionCatalogResourceFromEntityAssemblerTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    private final ResourceBundleMessageSource messageSource = messageSource();
    private final String plotId = UUID.randomUUID().toString();

    @Test
    @DisplayName("Formats the index with two decimals and the percents as integers, per locale")
    void formatsFiguresPerLocale() {
        var catalog = catalog(
                enabled(QuestionId.ALTERNATION_WHY_INDEX, "calendar", CatalogText.of("assistant.question.alternation.why_index.label", 0.51)),
                enabled(QuestionId.ALTERNATION_NEXT_YEAR, "harvest", CatalogText.of("assistant.question.alternation.next_year.label", "2027")),
                enabled(QuestionId.PLAN_WHY_PERCENT, "prio", CatalogText.of("assistant.question.plan.why_percent.label", 30)),
                enabled(QuestionId.PLAN_IF_LESS, "pruning", CatalogText.of("assistant.question.plan.if_less.label", 20)));

        assertThat(labels(catalog, ES)).containsExactly(
                "¿Por qué mi índice es 0,51?", "¿Cuánto cosecharé en 2027?", "¿Por qué 30 %?", "¿Y si aclaro solo 20 %?");
        assertThat(labels(catalog, Locale.ENGLISH)).containsExactly(
                "Why is my index 0.51?", "How much will I harvest in 2027?", "Why 30%?", "What if I thin only 20%?");
    }

    @Test
    @DisplayName("Localizes the fixed labels of both screens")
    void localizesFixedLabels() {
        var catalog = catalog(
                enabled(QuestionId.ALTERNATION_WHY_INDEX, "calendar", CatalogText.of("assistant.question.alternation.why_index.label_without_index")),
                enabled(QuestionId.ALTERNATION_BREAK_CYCLE, "pruning", CatalogText.of("assistant.question.alternation.break_cycle.label")),
                enabled(QuestionId.ALTERNATION_VS_CAMPAIGNS, "history", CatalogText.of("assistant.question.alternation.vs_campaigns.label")),
                enabled(QuestionId.PLAN_WHY_PERCENT, "prio", CatalogText.of("assistant.question.plan.why_percent.label_without_percent")),
                enabled(QuestionId.PLAN_IF_LESS, "pruning", CatalogText.of("assistant.question.plan.if_less.label_without_percent")),
                enabled(QuestionId.PLAN_IF_NONE, "alert", CatalogText.of("assistant.question.plan.if_none.label")),
                enabled(QuestionId.PLAN_BEST_TIME, "calendar", CatalogText.of("assistant.question.plan.best_time.label")));

        assertThat(labels(catalog, ES)).containsExactly(
                "¿Qué es mi índice de vecería?", "¿Qué hago para romper la vecería?",
                "¿Cómo va este lote frente a mis campañas?", "¿Por qué ese porcentaje de aclareo?",
                "¿Y si aclaro menos?", "¿Y si no aclaro?", "¿Cuándo es mejor aclarear?");
        assertThat(labels(catalog, Locale.ENGLISH)).doesNotHaveDuplicates().allMatch(label -> label.endsWith("?"));
    }

    @Test
    @DisplayName("Localizes every disabled reason, with the singular for one tree, and keeps null when enabled")
    void localizesDisabledReasons() {
        var label = CatalogText.of("assistant.question.plan.if_none.label");
        var catalog = catalog(
                disabled(CatalogText.of("assistant.reason.bbi_missing"), label),
                disabled(CatalogText.of("assistant.reason.campaigns_missing"), label),
                disabled(CatalogText.of("assistant.reason.caliber_missing"), label),
                disabled(CatalogText.of("assistant.reason.trees_missing.other", 3), label),
                disabled(CatalogText.of("assistant.reason.trees_missing.one"), label),
                disabled(CatalogText.of("assistant.reason.plan_missing"), label),
                enabled(QuestionId.PLAN_IF_NONE, "alert", label));

        var es = QuestionCatalogResourceFromEntityAssembler.toResource(catalog, messageSource, ES).questions();
        assertThat(es).extracting(QuestionResource::disabledReason).containsExactly(
                "Faltan cosechas para calcular el índice", "Registra al menos 2 campañas",
                "Faltan datos de calibre · registra un muestreo", "Faltan 3 árboles", "Falta 1 árbol",
                "Sin plan de aclareo activo", null);
        var en = QuestionCatalogResourceFromEntityAssembler.toResource(catalog, messageSource, Locale.ENGLISH).questions();
        assertThat(en).extracting(QuestionResource::disabledReason).containsExactly(
                "Harvests are missing to calculate the index", "Record at least 2 campaigns",
                "Caliber data is missing · record a sampling", "3 trees missing", "1 tree missing",
                "No active thinning plan", null);
    }

    @Test
    @DisplayName("Copies the screen, plot, ids, icons and flags")
    void copiesTheShape() {
        var catalog = catalog(enabled(QuestionId.PLAN_IF_NONE, "alert", CatalogText.of("assistant.question.plan.if_none.label")));

        var resource = QuestionCatalogResourceFromEntityAssembler.toResource(catalog, messageSource, ES);

        assertThat(resource.screen()).isEqualTo("PLAN");
        assertThat(resource.plotId()).isEqualTo(plotId);
        assertThat(resource.questions()).containsExactly(
                new QuestionResource("plan.if_none", "¿Y si no aclaro?", "alert", true, null));
    }

    private List<String> labels(ScreenQuestionCatalog catalog, Locale locale) {
        return QuestionCatalogResourceFromEntityAssembler.toResource(catalog, messageSource, locale).questions()
                .stream().map(QuestionResource::label).toList();
    }

    private ScreenQuestionCatalog catalog(CatalogQuestion... questions) {
        return new ScreenQuestionCatalog(ScreenKey.PLAN, new PlotId(plotId), List.of(questions));
    }

    private static CatalogQuestion enabled(QuestionId id, String icon, CatalogText label) {
        return new CatalogQuestion(id, label, icon, true, null);
    }

    private static CatalogQuestion disabled(CatalogText reason, CatalogText label) {
        return new CatalogQuestion(QuestionId.PLAN_IF_NONE, label, "alert", false, reason);
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
