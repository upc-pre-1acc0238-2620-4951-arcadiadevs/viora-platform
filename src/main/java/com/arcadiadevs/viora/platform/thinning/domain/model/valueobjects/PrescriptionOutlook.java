package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * What the thinning plan of a plot shows besides the prescription itself: the crop load before thinning against
 * the sustainable target (US26), the sampling progress, the offsets of the technical profile and the yield.
 *
 * @param currentFruitsPerShoot      mean fruits per shoot of the sampling; null until it is representative
 * @param targetFruitsPerShoot       sustainable target of the prescription, or of the variety profile while it is
 *                                   not issued; null when the variety has no profile
 * @param loadRatio                  current load divided by the target; null when either is unknown
 * @param loadState                  state of the current load against the target; null when either is unknown
 * @param sampledTrees               distinct trees sampled in the campaign
 * @param treesNeeded                trees still missing for a representative sampling
 * @param lastSampledOn              date of the latest tree sampled, or null without samplings
 * @param windowOpensDaysAfterBloom  profile offset of the window opening; null when the variety has no profile
 * @param windowClosesDaysAfterBloom profile offset of the window closing; null when the variety has no profile
 * @param yieldEstimate              sustainable and estimated yield of the campaign
 */
public record PrescriptionOutlook(
        Double currentFruitsPerShoot,
        Double targetFruitsPerShoot,
        Double loadRatio,
        LoadState loadState,
        int sampledTrees,
        int treesNeeded,
        LocalDate lastSampledOn,
        Integer windowOpensDaysAfterBloom,
        Integer windowClosesDaysAfterBloom,
        YieldEstimate yieldEstimate
) {

    public PrescriptionOutlook {
        if (sampledTrees < 0 || treesNeeded < 0) {
            throw new IllegalArgumentException("thinning.outlook.trees.negative");
        }
        if (yieldEstimate == null) {
            throw new IllegalArgumentException("thinning.outlook.yield.null");
        }
    }
}
