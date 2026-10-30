package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Version of the data an answer was built from: {@code sha256:} plus the hex digest of the facts serialized with
 * sorted keys. Two answers with the same facts share the version, whatever their wording.
 *
 * @param value the version, for example {@code sha256:3f2a...}
 */
public record DataVersion(String value) {

    private static final String PREFIX = "sha256:";

    public DataVersion {
        if (value == null || !value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("assistant.data_version.invalid");
        }
    }

    /**
     * Computes the version of a set of facts.
     *
     * @param facts the facts by name; values are numbers, texts, booleans or null
     * @return the version
     */
    public static DataVersion of(Map<String, ?> facts) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical(facts).getBytes(StandardCharsets.UTF_8));
            return new DataVersion(PREFIX + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** Flat JSON-like text with sorted keys; 0.4 and 0.40, or 30 and 30.0, serialize alike. */
    static String canonical(Map<String, ?> facts) {
        var sorted = new TreeMap<String, Object>(facts == null ? Map.of() : facts);
        var out = new StringBuilder("{");
        sorted.forEach((key, value) -> {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append(quote(key)).append(':').append(valueOf(value));
        });
        return out.append('}').toString();
    }

    private static String valueOf(Object value) {
        return switch (value) {
            case null -> "null";
            case Boolean bool -> bool.toString();
            case Number number -> new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
            default -> quote(value.toString());
        };
    }

    private static String quote(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
