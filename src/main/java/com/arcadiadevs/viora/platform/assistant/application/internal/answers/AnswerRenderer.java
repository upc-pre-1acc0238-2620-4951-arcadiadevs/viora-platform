package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Writes the deterministic texts of an answer from its facts with the message templates of a language
 * ({@code assistant.template.*}). The message decides how numbers look, so Spanish reads "0,57" and English "0.57".
 */
@Component
public class AnswerRenderer {

    private static final String DATE = "assistant.template.date";
    private static final String DATE_SHORT = "assistant.template.date_short";

    private final MessageSource messageSource;

    public AnswerRenderer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Renders the texts of an answer.
     *
     * @param facts  the facts of the answer
     * @param locale the language
     * @return text, cards, sources and actions in that language
     */
    public RenderedAnswer render(AssembledFacts facts, Locale locale) {
        return new RenderedAnswer(
                resolve(facts.text(), locale),
                facts.cards().stream()
                        .map(card -> new AnswerFact(resolve(card.value(), locale), resolve(card.label(), locale),
                                resolve(card.pill(), locale), card.tone()))
                        .toList(),
                facts.sources().stream().map(source -> new AnswerSource(resolve(source, locale))).toList(),
                facts.actions().stream()
                        .map(action -> new AnswerAction(action.type(), resolve(action.label(), locale),
                                action.target()))
                        .toList());
    }

    /**
     * Resolves one text, its nested texts and dates first.
     *
     * @param text   the text
     * @param locale the language
     * @return the localized text
     */
    public String resolve(CatalogText text, Locale locale) {
        var args = text.args().stream().map(arg -> argument(arg, locale)).toArray();
        return messageSource.getMessage(text.key(), args, text.key(), locale);
    }

    private Object argument(Object arg, Locale locale) {
        return switch (arg) {
            case CatalogText nested -> resolve(nested, locale);
            case TextDate date -> DateTimeFormatter
                    .ofPattern(messageSource.getMessage(date.shortForm() ? DATE_SHORT : DATE, null, locale), locale)
                    .format(date.date());
            case null, default -> arg;
        };
    }
}
