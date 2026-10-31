package com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.QuestionCatalogResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.QuestionResource;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * Assembler turning the evaluated catalog into its REST resource, localizing every text with the language of the
 * request. The figures are formatted here, so Spanish reads "0,51" and "30 %" and English "0.51" and "30%".
 */
@NullMarked
public final class QuestionCatalogResourceFromEntityAssembler {

    private QuestionCatalogResourceFromEntityAssembler() {
    }

    /**
     * Converts the catalog into its resource.
     *
     * @param catalog       the evaluated catalog
     * @param messageSource the i18n message source
     * @param locale        the language of the request
     * @return the resource
     */
    public static QuestionCatalogResource toResource(ScreenQuestionCatalog catalog, MessageSource messageSource,
            Locale locale) {
        return new QuestionCatalogResource(
                catalog.screen().name(),
                catalog.plotId() == null ? null : catalog.plotId().plotId(),
                catalog.questions().stream().map(question -> toResource(question, messageSource, locale)).toList());
    }

    private static QuestionResource toResource(CatalogQuestion question, MessageSource messageSource, Locale locale) {
        return new QuestionResource(
                question.questionId().value(),
                localize(question.label(), messageSource, locale),
                question.icon(),
                question.enabled(),
                question.disabledReason() == null ? null : localize(question.disabledReason(), messageSource, locale));
    }

    private static String localize(CatalogText text, MessageSource messageSource, Locale locale) {
        return messageSource.getMessage(text.key(), text.args().toArray(), text.key(), locale);
    }
}
