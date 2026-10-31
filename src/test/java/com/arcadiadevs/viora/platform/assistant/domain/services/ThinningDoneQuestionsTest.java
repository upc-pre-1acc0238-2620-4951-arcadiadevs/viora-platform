package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningDoneFacts;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ThinningDoneSamples.contract;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ThinningDoneSamples.notRecorded;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThinningDoneQuestions: the questions and answers of the screen shown after the thinning is recorded")
class ThinningDoneQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final List<QuestionId> QUESTIONS = List.of(QuestionId.THINNING_HARVEST_CHANGE,
            QuestionId.THINNING_WHY_TARGET, QuestionId.THINNING_NEXT_SAMPLING);
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("THINNING_DONE is a plot screen whose questions use the contract prefix thinning.")
    void screen() {
        assertThat(ScreenKey.fromValue("thinning_done")).isEqualTo(ScreenKey.THINNING_DONE);
        assertThat(ScreenKey.THINNING_DONE.requiresPlot()).isTrue();
        assertThat(ScreenKey.THINNING_DONE.questionPrefix()).isEqualTo("thinning.");
        assertThat(QUESTIONS).extracting(QuestionId::value).containsExactly("thinning.harvest_change",
                "thinning.why_target", "thinning.next_sampling");
        assertThat(QUESTIONS).allMatch(question -> question.screen() == ScreenKey.THINNING_DONE);
        assertThat(contract().facts().screen()).isEqualTo(ScreenKey.THINNING_DONE);
    }

    // --- Catalog: enabled flags and disabled reasons ---------------------------------------------------------------

    @Test
    @DisplayName("catalog with the contract figures: three questions in order, all enabled, the target in the label")
    void catalogRecorded() {
        var questions = QuestionCatalog.questionsFor(contract().facts());

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "thinning.harvest_change", "thinning.why_target", "thinning.next_sampling");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("harvest", "prio", "calendar");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsOnlyNulls();
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.thinning.harvest_change.label"),
                CatalogText.of("assistant.question.thinning.why_target.label", 0.4),
                CatalogText.of("assistant.question.thinning.next_sampling.label"));
        assertThat(questions).extracting(q -> localizer.resolve(q.label(), SPANISH)).containsExactly(
                "¿Cómo cambia mi cosecha?", "¿Por qué el objetivo es 0,40?", "¿Cuándo vuelvo a muestrear?");
        assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.ENGLISH)).containsExactly(
                "How does my harvest change?", "Why is the target 0.40?", "When do I sample again?");
    }

    @Test
    @DisplayName("catalog before the thinning is recorded: every question disabled, no answer is written")
    void catalogNotRecorded() {
        var questions = QuestionCatalog.questionsFor(notRecorded());

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "thinning.harvest_change", "thinning.why_target", "thinning.next_sampling");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("harvest", "prio", "calendar");
        assertThat(questions).noneMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::disabledReason)
                .containsOnly(CatalogText.of("assistant.reason.thinning_missing"));
        assertThat(localizer.resolve(questions.get(0).disabledReason(), SPANISH))
                .isEqualTo("Sin aclareo registrado en esta campaña");
        assertThat(localizer.resolve(questions.get(0).disabledReason(), Locale.ENGLISH))
                .isEqualTo("No thinning recorded this campaign");
        assertThat(localizer.resolve(questions.get(1).label(), SPANISH)).isEqualTo("¿Por qué ese objetivo de carga?");
        assertThat(localizer.resolve(questions.get(1).label(), Locale.ENGLISH)).isEqualTo("Why that load target?");
        for (var question : QUESTIONS) {
            assertThat(AnswerComposer.compose(question, notRecorded(), PLOT)).as(question.value()).isEmpty();
        }
    }

    @Test
    @DisplayName("harvest_change needs the yield after the thinning: without it, disabled for missing harvests or area")
    void catalogWithoutYield() {
        var facts = contract().yield(null, null, null).facts();
        var questions = QuestionCatalog.questionsFor(facts);

        assertThat(questions.get(0).enabled()).isFalse();
        assertThat(questions.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.yield_missing"));
        assertThat(localizer.resolve(questions.get(0).disabledReason(), SPANISH))
                .isEqualTo("Faltan cosechas registradas o la superficie del lote para estimar la cosecha");
        assertThat(localizer.resolve(questions.get(0).disabledReason(), Locale.ENGLISH))
                .isEqualTo("Recorded harvests or the plot area are missing to estimate the harvest");
        assertThat(questions.get(1).enabled()).isTrue();
        assertThat(questions.get(2).enabled()).isTrue();
        assertThat(AnswerComposer.compose(QuestionId.THINNING_HARVEST_CHANGE, facts, PLOT)).isEmpty();
        // The yield without thinning alone does not enable it either
        assertThat(QuestionCatalog.questionsFor(contract().yield(11700.0, null, "FRUIT_COUNT_ONLY").facts())
                .get(0).enabled()).isFalse();
    }

    @Test
    @DisplayName("why_target needs the target load: without it, disabled for missing data and labelled without figure")
    void catalogWithoutTarget() {
        var facts = contract().load(0.5714, 0.4, null, "BALANCED").facts();
        var questions = QuestionCatalog.questionsFor(facts);

        assertThat(questions.get(1).enabled()).isFalse();
        assertThat(questions.get(1).label())
                .isEqualTo(CatalogText.of("assistant.question.thinning.why_target.label_without_target"));
        assertThat(questions.get(1).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.facts_missing"));
        assertThat(localizer.resolve(questions.get(1).disabledReason(), SPANISH))
                .isEqualTo("Faltan datos para responder esta pregunta");
        assertThat(localizer.resolve(questions.get(1).disabledReason(), Locale.ENGLISH))
                .isEqualTo("Data is missing to answer this question");
        assertThat(questions.get(0).enabled()).isTrue();
        assertThat(questions.get(2).enabled()).isTrue();
        assertThat(AnswerComposer.compose(QuestionId.THINNING_WHY_TARGET, facts, PLOT)).isEmpty();
    }

    @Test
    @DisplayName("next_sampling is offered once the thinning is recorded, even with no other figure")
    void catalogOnlyTheDay() {
        var facts = contract().removed(null).load(null, null, null, null).yield(null, null, null).profile(null)
                .window(null, null).facts();
        var questions = QuestionCatalog.questionsFor(facts);

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, false, true);
        assertThat(questions.get(2).disabledReason()).isNull();
        assertThat(AnswerComposer.compose(QuestionId.THINNING_NEXT_SAMPLING, facts, PLOT)).isPresent();
    }

    @Test
    @DisplayName("a question of another screen is not answered with the thinning figures")
    void foreignQuestion() {
        assertThat(contract().facts().answer(QuestionId.ALERT_WHAT_TO_DO, PLOT)).isEmpty();
        assertThat(contract().facts().answer(QuestionId.CHILL_MISSING, PLOT)).isEmpty();
    }

    // --- harvest_change ------------------------------------------------------------------------------------------

    @Test
    @DisplayName("harvest_change counting fruits only: the yield without thinning is a ceiling, load balanced, es and en")
    void harvestChangeCeiling() {
        var es = answer(QuestionId.THINNING_HARVEST_CHANGE, contract().facts(), ES);
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por hectárea para "
                + "esta campaña. Sin aclarear la cifra llegaría a 11.700 kg, pero es un techo: con más fruta el calibre "
                + "baja y Viora aún no lo mide para tu variedad. La carga quedó en 0,40 frutos por brote, dentro del "
                + "objetivo de 0,40.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("8.200", "kg por hectárea estimados", "por número de frutos", FactTone.PRIMARY),
                new AnswerFact("0,40", "frutos por brote tras el aclareo", "objetivo 0,40", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el aclareo registrado el 28 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.THINNING_HARVEST_CHANGE, contract().facts(), EN);
        assertThat(en.text()).isEqualTo("With the load your thinning left, Viora estimates 8,200 kg per hectare for "
                + "this campaign. Without thinning the figure would reach 11,700 kg, but it is a ceiling: with more "
                + "fruit the caliber drops and Viora does not measure it for your variety yet. The load ended at 0.40 "
                + "fruits per shoot, within the target of 0.40.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("8,200", "estimated kg per hectare", "by number of fruits", FactTone.PRIMARY),
                new AnswerFact("0.40", "fruits per shoot after thinning", "target 0.40", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the thinning recorded on October 28");
        assertThat(en.actions()).isEmpty();
    }

    @Test
    @DisplayName("harvest_change never claims the next harvest: no 2027 and no stability, in es and en")
    void harvestChangeSaysNothingAboutNextCampaign() {
        for (var language : List.of(ES, EN)) {
            var answer = answer(QuestionId.THINNING_HARVEST_CHANGE, contract().facts(), language);
            var texts = new ArrayList<>(List.of(answer.text()));
            answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
            assertThat(texts).allSatisfy(text -> assertThat(text).doesNotContain("2027").doesNotContainIgnoringCase(
                    "estable").doesNotContainIgnoringCase("stable"));
        }
    }

    @Test
    @DisplayName("harvest_change with a caliber-adjusted yield: compared against without thinning, moderate overload")
    void harvestChangeCompared() {
        var facts = contract().yield(11700.0, 8200.0, "CALIBER_ADJUSTED")
                .load(0.5714, 0.47, 0.4, "MODERATE_OVERLOAD").facts();

        var es = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, ES);
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por hectárea para "
                + "esta campaña, frente a 11.700 kg si no hubieras aclareado. La carga quedó en 0,47 frutos por brote, "
                + "hasta un 30 % sobre el objetivo de 0,40.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("8.200", "kg por hectárea estimados", "en calibración", FactTone.PRIMARY),
                new AnswerFact("0,47", "frutos por brote tras el aclareo", "objetivo 0,40", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, EN);
        assertThat(en.text()).isEqualTo("With the load your thinning left, Viora estimates 8,200 kg per hectare for "
                + "this campaign, against 11,700 kg if you had not thinned. The load ended at 0.47 fruits per shoot, "
                + "up to 30 % over the target of 0.40.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("8,200", "estimated kg per hectare", "calibrating", FactTone.PRIMARY),
                new AnswerFact("0.47", "fruits per shoot after thinning", "target 0.40", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("harvest_change without the yield before thinning: the estimate alone, severe overload, es and en")
    void harvestChangeOnly() {
        var facts = contract().yield(null, 8200.0, "CALIBER_ADJUSTED")
                .load(0.9, 0.6, 0.4, "SEVERE_OVERLOAD").facts();

        var es = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, ES);
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por hectárea para la "
                + "cosecha de esta campaña. La carga quedó en 0,60 frutos por brote, más de un 30 % sobre el objetivo "
                + "de 0,40: el árbol sigue sobrecargado.");
        assertThat(es.facts().get(1)).isEqualTo(
                new AnswerFact("0,60", "frutos por brote tras el aclareo", "objetivo 0,40", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, EN);
        assertThat(en.text()).isEqualTo("With the load your thinning left, Viora estimates 8,200 kg per hectare for "
                + "the harvest of this campaign. The load ended at 0.60 fruits per shoot, more than 30 % over the "
                + "target of 0.40: the tree is still overloaded.");
        assertThat(en.facts().get(1)).isEqualTo(
                new AnswerFact("0.60", "fruits per shoot after thinning", "target 0.40", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("harvest_change without the load balance (or an unknown state): the yield alone and the thinning day")
    void harvestChangeWithoutLoad() {
        var facts = contract().load(null, null, null, null).facts();

        var es = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, ES);
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por hectárea para "
                + "esta campaña. Sin aclarear la cifra llegaría a 11.700 kg, pero es un techo: con más fruta el calibre "
                + "baja y Viora aún no lo mide para tu variedad.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("8.200", "kg por hectárea estimados", "por número de frutos", FactTone.PRIMARY),
                new AnswerFact("28 oct", "aclareo de esta campaña", "aclareo registrado", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.THINNING_HARVEST_CHANGE, facts, EN);
        assertThat(en.text()).isEqualTo("With the load your thinning left, Viora estimates 8,200 kg per hectare for "
                + "this campaign. Without thinning the figure would reach 11,700 kg, but it is a ceiling: with more "
                + "fruit the caliber drops and Viora does not measure it for your variety yet.");
        assertThat(en.facts().get(1)).isEqualTo(
                new AnswerFact("Oct 28", "thinning this campaign", "thinning recorded", FactTone.HIGHLIGHT));

        // A load state Thinning may add later reads as no load balance, never as a raw key
        var unknownState = contract().load(0.5714, 0.4, 0.4, "UNDERLOADED").facts();
        assertThat(answer(QuestionId.THINNING_HARVEST_CHANGE, unknownState, ES).text()).isEqualTo(es.text());
        // The load sentence needs the residual load and the target too
        var withoutResidual = contract().load(0.5714, null, 0.4, "BALANCED").facts();
        assertThat(answer(QuestionId.THINNING_HARVEST_CHANGE, withoutResidual, EN).text()).isEqualTo(en.text());
    }

    // --- why_target ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("why_target: the target of the variety profile, who approved it and where the thinning left it")
    void whyTarget() {
        var es = answer(QuestionId.THINNING_WHY_TARGET, contract().facts(), ES);
        assertThat(es.text()).isEqualTo("El objetivo de 0,40 frutos por brote es la carga que el árbol puede sostener "
                + "según el perfil de aclareo de tu variedad. Ese perfil es de demostración y no lo validó un agrónomo. "
                + "Tu muestreo midió 0,57, quitaste el 30 % de los frutos y la carga quedó en 0,40.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0,40", "objetivo de frutos por brote", "demostración", FactTone.PRIMARY),
                new AnswerFact("0,40", "frutos por brote tras el aclareo", "quitaste 30 %", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el aclareo registrado el 28 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.THINNING_WHY_TARGET, contract().facts(), EN);
        assertThat(en.text()).isEqualTo("The target of 0.40 fruits per shoot is the load the tree can sustain "
                + "according to the thinning profile of your variety. That profile is a demonstration one and no "
                + "agronomist validated it. Your sampling measured 0.57, you removed 30 % of the fruits and the load "
                + "ended at 0.40.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("0.40", "target fruits per shoot", "demonstration", FactTone.PRIMARY),
                new AnswerFact("0.40", "fruits per shoot after thinning", "you removed 30 %", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("why_target: an approved, a provisional and an unknown profile each say so, in es and en")
    void whyTargetProfiles() {
        var approved = contract().profile("AGRONOMIST_APPROVED").facts();
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, approved, ES).text())
                .contains(" Ese perfil está aprobado por un agrónomo. ");
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, approved, ES).facts().get(0).pill())
                .isEqualTo("aprobado por agrónomo");
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, approved, EN).text())
                .contains(" That profile is approved by an agronomist. ");
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, approved, EN).facts().get(0).pill())
                .isEqualTo("agronomist approved");

        var provisional = contract().profile("PROVISIONAL").facts();
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, provisional, ES).text())
                .contains(" Ese perfil es provisional: aún no lo valida un agrónomo. ");
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, provisional, ES).facts().get(0).pill())
                .isEqualTo("provisional");
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, provisional, EN).text())
                .contains(" That profile is provisional: an agronomist has not validated it yet. ");

        for (var status : new String[]{null, "RETIRED"}) {
            var unknown = contract().profile(status).facts();
            var es = answer(QuestionId.THINNING_WHY_TARGET, unknown, ES);
            assertThat(es.text()).as(status).contains(" Viora no tiene registrado quién aprobó ese perfil. ");
            assertThat(es.facts().get(0)).as(status).isEqualTo(
                    new AnswerFact("0,40", "objetivo de frutos por brote", "perfil de la variedad", FactTone.PRIMARY));
            var en = answer(QuestionId.THINNING_WHY_TARGET, unknown, EN);
            assertThat(en.text()).as(status).contains(" Viora has no record of who approved that profile. ");
            assertThat(en.facts().get(0).pill()).as(status).isEqualTo("variety profile");
        }
    }

    @Test
    @DisplayName("why_target without the removal reported: the target and the profile, and the thinning day")
    void whyTargetShort() {
        var facts = contract().removed(null).facts();

        var es = answer(QuestionId.THINNING_WHY_TARGET, facts, ES);
        assertThat(es.text()).isEqualTo("El objetivo de 0,40 frutos por brote es la carga que el árbol puede sostener "
                + "según el perfil de aclareo de tu variedad. Ese perfil es de demostración y no lo validó un "
                + "agrónomo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0,40", "objetivo de frutos por brote", "demostración", FactTone.PRIMARY),
                new AnswerFact("Registrado", "aclareo de esta campaña", "el 28 oct", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.THINNING_WHY_TARGET, facts, EN);
        assertThat(en.text()).isEqualTo("The target of 0.40 fruits per shoot is the load the tree can sustain "
                + "according to the thinning profile of your variety. That profile is a demonstration one and no "
                + "agronomist validated it.");
        assertThat(en.facts().get(1)).isEqualTo(
                new AnswerFact("Recorded", "thinning this campaign", "on Oct 28", FactTone.HIGHLIGHT));

        // The load before or after the thinning missing also leaves the result out
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, contract().load(null, 0.4, 0.4, "BALANCED").facts(), ES)
                .text()).isEqualTo(es.text());
        assertThat(answer(QuestionId.THINNING_WHY_TARGET, contract().load(0.5714, null, 0.4, "BALANCED").facts(), EN)
                .text()).isEqualTo(en.text());
    }

    // --- next_sampling -------------------------------------------------------------------------------------------

    @Test
    @DisplayName("next_sampling: none this campaign; next campaign after full bloom, within the profile window")
    void nextSamplingWithWindow() {
        var es = answer(QuestionId.THINNING_NEXT_SAMPLING, contract().facts(), ES);
        assertThat(es.text()).isEqualTo("Ya registraste el aclareo el 28 de octubre, así que esta campaña no pide más "
                + "muestreos. En la campaña 2027, registra la plena floración de tu lote y muestrea antes de que abra la "
                + "ventana de aclareo, que en el perfil de tu variedad va de 14 a 49 días después de la floración.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("2027", "próximo muestreo", "tras la plena floración", FactTone.PRIMARY),
                new AnswerFact("14–49", "días después de la floración", "ventana de aclareo", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el aclareo registrado el 28 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.THINNING_NEXT_SAMPLING, contract().facts(), EN);
        assertThat(en.text()).isEqualTo("You already recorded the thinning on October 28, so this campaign needs no "
                + "more sampling. In the 2027 campaign, record the full bloom of your plot and sample before the "
                + "thinning window opens, which in the profile of your variety runs from 14 to 49 days after bloom.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("2027", "next sampling", "after full bloom", FactTone.PRIMARY),
                new AnswerFact("14–49", "days after bloom", "thinning window", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next_sampling without the profile window: no date and no window, the thinning day instead")
    void nextSamplingWithoutWindow() {
        var facts = contract().window(14, null).facts();

        var es = answer(QuestionId.THINNING_NEXT_SAMPLING, facts, ES);
        assertThat(es.text()).isEqualTo("Ya registraste el aclareo el 28 de octubre, así que esta campaña no pide más "
                + "muestreos. En la campaña 2027, registra la plena floración de tu lote y vuelve a muestrear antes del "
                + "aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("2027", "próximo muestreo", "tras la plena floración", FactTone.PRIMARY),
                new AnswerFact("Registrado", "aclareo de esta campaña", "el 28 oct", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.THINNING_NEXT_SAMPLING, facts, EN);
        assertThat(en.text()).isEqualTo("You already recorded the thinning on October 28, so this campaign needs no "
                + "more sampling. In the 2027 campaign, record the full bloom of your plot and sample again before "
                + "thinning.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("2027", "next sampling", "after full bloom", FactTone.PRIMARY),
                new AnswerFact("Recorded", "thinning this campaign", "on Oct 28", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.THINNING_NEXT_SAMPLING, contract().window(null, 49).facts(), EN).text())
                .isEqualTo(en.text());
    }

    // --- Templates against the validator -------------------------------------------------------------------------

    @Test
    @DisplayName("every variant resolves in es and en, has two cards and passes the validator as its own rephrasing")
    void everyTemplatePassesAsItsOwnRephrasing() {
        var scenarios = List.of(contract().facts(),
                contract().yield(11700.0, 8200.0, "CALIBER_ADJUSTED").load(0.5714, 0.47, 0.4, "MODERATE_OVERLOAD")
                        .profile("AGRONOMIST_APPROVED").facts(),
                contract().yield(null, 8200.0, "CALIBER_ADJUSTED").load(0.9, 0.6, 0.4, "SEVERE_OVERLOAD")
                        .profile("PROVISIONAL").window(null, null).facts(),
                contract().load(0.5714, 0.4, 0.4, null).removed(null).profile(null).facts());
        var variants = new HashSet<String>();
        for (var facts : scenarios) {
            for (var question : QUESTIONS) {
                var draft = AnswerComposer.compose(question, facts, PLOT).orElseThrow();
                variants.add(draft.text().key());
                for (var language : List.of(ES, EN)) {
                    var answer = localizer.localize(draft, language);
                    var where = draft.text().key() + " [" + language.code() + "]: " + answer.text();
                    var texts = new ArrayList<>(List.of(answer.text()));
                    answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                    answer.sources().forEach(source -> texts.add(source.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).as(where).hasSize(2);
                    assertThat(AnswerTextValidator.validate(answer.text(), answer.text(), answer.facts(), language))
                            .as(where).isEqualTo(Verdict.ACCEPTED);
                }
            }
        }
        // harvest_change: text and text.single; why_target: text and text.short; next_sampling: text and text.window
        assertThat(variants).containsExactlyInAnyOrder(
                "assistant.answer.thinning.harvest_change.text", "assistant.answer.thinning.harvest_change.text.single",
                "assistant.answer.thinning.why_target.text", "assistant.answer.thinning.why_target.text.short",
                "assistant.answer.thinning.next_sampling.text", "assistant.answer.thinning.next_sampling.text.window");
    }

    @Test
    @DisplayName("harvest_change: a faithful rephrasing passes; claiming a steadier 2027 keeps the template, es and en")
    void nextHarvestIsNeverAddedByTheModel() {
        var draft = AnswerComposer.compose(QuestionId.THINNING_HARVEST_CHANGE, contract().facts(), PLOT).orElseThrow();
        var es = localizer.localize(draft, ES);
        var en = localizer.localize(draft, EN);

        assertThat(AnswerTextValidator.validate("Tras tu aclareo, Viora estima 8.200 kg por hectárea esta campaña; "
                + "sin aclarear serían como mucho 11.700 kg. La carga quedó en 0,40, dentro del objetivo de 0,40.",
                es.text(), es.facts(), ES)).isEqualTo(Verdict.ACCEPTED);
        assertThat(AnswerTextValidator.validate("Tras tu aclareo, Viora estima 8.200 kg por hectárea esta campaña y "
                + "una cosecha 2027 más estable.", es.text(), es.facts(), ES)).isEqualTo(Verdict.UNKNOWN_NUMBER);

        assertThat(AnswerTextValidator.validate("After your thinning, Viora estimates 8,200 kg per hectare this "
                + "campaign; without thinning it would be at most 11,700 kg. The load ended at 0.40, within the target "
                + "of 0.40.", en.text(), en.facts(), EN)).isEqualTo(Verdict.ACCEPTED);
        assertThat(AnswerTextValidator.validate("After your thinning, Viora estimates 8,200 kg per hectare this "
                + "campaign and a steadier 2027 harvest.", en.text(), en.facts(), EN))
                .isEqualTo(Verdict.UNKNOWN_NUMBER);
    }

    // --- Data version ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("the data version changes with the thinning outcome and the campaign, not between equal figures")
    void dataVersion() {
        var version = AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE, contract().facts());

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE, contract().facts())).isEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE, notRecorded())).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE,
                contract().load(0.5714, 0.47, 0.4, "MODERATE_OVERLOAD").facts())).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE,
                contract().yield(11700.0, 8100.0, "FRUIT_COUNT_ONLY").facts())).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE, contract().window(15, 49).facts()))
                .isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.THINNING_DONE,
                new ThinningDoneFacts(2027, contract().build()))).isNotEqualTo(version);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, ThinningDoneFacts facts, AnswerLanguage language) {
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
