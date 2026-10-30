package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Arrays;
import java.util.List;

/**
 * A text of the catalog not yet localized: the message key and the figures it shows. The REST layer resolves it
 * with the language of the request, so the domain never formats numbers.
 *
 * @param key  message key under {@code assistant.}
 * @param args figures of the message, in order
 */
public record CatalogText(String key, List<Object> args) {

    public CatalogText {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("assistant.text.key.null");
        }
        args = args == null ? List.of() : List.copyOf(args);
    }

    /**
     * Creates a text.
     *
     * @param key  message key
     * @param args figures of the message
     * @return the text
     */
    public static CatalogText of(String key, Object... args) {
        return new CatalogText(key, Arrays.asList(args));
    }
}
