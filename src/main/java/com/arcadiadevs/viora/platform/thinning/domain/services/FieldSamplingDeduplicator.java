package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TreeTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain Service responsible for validating that field sampling trees do not contain duplicates.
 */
public final class FieldSamplingDeduplicator {

    private FieldSamplingDeduplicator() {
    }

    /**
     * Validates that the provided batch of tree records contains no internal duplicate tree tags.
     *
     * @param records the tree sampling records to evaluate
     * @return the verified list of records
     * @throws IllegalArgumentException if duplicate tree tags are detected
     */
    public static List<TreeSamplingRecord> validateNoDuplicates(List<TreeSamplingRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("thinning.samples.empty");
        }
        Set<TreeTag> seenTags = new HashSet<>();
        for (TreeSamplingRecord record : records) {
            if (!seenTags.add(record.treeTag())) {
                throw new IllegalArgumentException("thinning.tree_tag.duplicate");
            }
        }
        return new ArrayList<>(records);
    }
}
