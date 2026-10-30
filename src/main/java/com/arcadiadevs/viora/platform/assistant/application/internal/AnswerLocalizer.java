package com.arcadiadevs.viora.platform.assistant.application.internal;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Writes an answer draft in a language. The figures arrive as numbers, dates and nested texts, and the message of the
 * language formats them: Spanish reads "0,57" and "14 nov", English "0.57" and "14 Nov". The domain never formats.
 */
@Component
public class AnswerLocalizer {

    private final MessageSource messageSource;

    /**
     * Constructs the localizer.
     *
     * @param messageSource the i18n message source
     */
    public AnswerLocalizer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * An answer draft written in one language.
     *
     * @param text    the sentences
     * @param facts   the two cards
     * @param sources what the answer is based on
     * @param actions the buttons
     */
    public record Localized(String text, List<AnswerFact> facts, List<AnswerSource> sources,
                            List<AnswerAction> actions) {
    }

    /**
     * Writes the draft.
     *
     * @param draft    the draft
     * @param language the language
     * @return the draft written in that language
     */
    public Localized localize(AnswerDraft draft, AnswerLanguage language) {
        var locale = language.locale();
        return new Localized(
                resolve(draft.text(), locale),
                draft.facts().stream().map(fact -> fact(fact, locale)).toList(),
                List.of(new AnswerSource(resolve(draft.source(), locale))),
                draft.actions().stream().map(action -> action(action, locale)).toList());
    }

    /**
     * Writes one text.
     *
     * @param text   the message key and its figures
     * @param locale the locale
     * @return the text; the key itself when the message does not exist
     */
    public String resolve(CatalogText text, Locale locale) {
        var args = text.args().stream().map(arg -> argument(arg, locale)).toArray();
        return messageSource.getMessage(text.key(), args, text.key(), locale);
    }

    private AnswerFact fact(FactDraft draft, Locale locale) {
        return new AnswerFact(resolve(draft.value(), locale), resolve(draft.label(), locale),
                resolve(draft.pill(), locale), draft.tone());
    }

    private AnswerAction action(ActionDraft draft, Locale locale) {
        return new AnswerAction(draft.type(), resolve(draft.label(), locale), draft.target());
    }

    private Object argument(Object arg, Locale locale) {
        if (arg instanceof CatalogText nested) {
            return resolve(nested, locale);
        }
        if (arg instanceof LocalDate date) {
            return DateTimeFormatter.ofPattern("d MMM", locale).format(date).replace(".", "");
        }
        return arg;
    }
}
