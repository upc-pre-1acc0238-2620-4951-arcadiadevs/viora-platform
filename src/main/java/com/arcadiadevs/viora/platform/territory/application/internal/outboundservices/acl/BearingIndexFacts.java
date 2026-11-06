package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

/**
 * The biennial bearing index of a plot, informative only.
 *
 * @param value    the index, from 0 to 1
 * @param category its category, such as high alternance
 */
public record BearingIndexFacts(double value, String category) {
}
