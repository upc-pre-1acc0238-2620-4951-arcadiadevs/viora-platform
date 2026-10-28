package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing the physical identifier or tag plate of an evaluated olive tree.
 *
 * @param value the physical tag identifier
 */
public record TreeTag(String value) {

    /** Longest tag stored, as long as the database column. */
    public static final int MAX_LENGTH = 50;

    /**
     * Compact constructor validating that treeTag is non-null and not blank.
     */
    public TreeTag {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.tree_tag.null_or_empty");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("thinning.tree_tag.too_long");
        }
    }
}
