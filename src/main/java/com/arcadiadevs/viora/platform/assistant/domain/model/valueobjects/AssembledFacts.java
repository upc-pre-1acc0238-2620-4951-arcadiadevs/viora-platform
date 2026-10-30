package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Everything a facts assembler produces for one question: the facts with the numbers already computed, the template
 * of the text, exactly two fact cards, the sources and the actions. Only the facts feed the data version, so a
 * wording change never makes an answer stale.
 *
 * @param facts   the facts, by name; every number a text may show is here
 * @param text    template of the answer text
 * @param cards   exactly two fact cards
 * @param sources what the answer is based on
 * @param actions links to existing screens; may be empty
 */
public record AssembledFacts(
        SortedMap<String, Object> facts,
        CatalogText text,
        List<FactCardTemplate> cards,
        List<CatalogText> sources,
        List<ActionTemplate> actions
) {

    /** The app shows two bento cards of the same size. */
    public static final int CARDS = 2;

    public AssembledFacts {
        if (facts == null || facts.isEmpty() || text == null) {
            throw new IllegalArgumentException("assistant.facts.invalid");
        }
        if (cards == null || cards.size() != CARDS) {
            throw new IllegalArgumentException("assistant.facts.cards");
        }
        facts = Collections.unmodifiableSortedMap(new TreeMap<>(facts));
        cards = List.copyOf(cards);
        sources = sources == null ? List.of() : List.copyOf(sources);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /**
     * Version of the data the facts were built from.
     *
     * @return the data version
     */
    public DataVersion dataVersion() {
        return DataVersion.of(facts);
    }

}
