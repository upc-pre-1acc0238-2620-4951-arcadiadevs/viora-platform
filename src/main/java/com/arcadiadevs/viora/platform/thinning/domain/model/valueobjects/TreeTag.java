package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing the physical identifier or tag plate of an evaluated olive tree.
 *
 * @param value the physical tag identifier
 */
public record TreeTag(String value) {

    /**
     * Compact constructor validating that treeTag is non-null and not blank.
     */
    public TreeTag {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.tree_tag.null_or_empty");
        }
        value = value.trim();
    }
}
