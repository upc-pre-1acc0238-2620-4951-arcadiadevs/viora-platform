package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read model of a member in the manager's roll. Who does not share shows only their name and state.
 *
 * @param id               the member, their user identifier
 * @param fullName         their name
 * @param sharing          {@code SHARING}, {@code WITHDRAWN} or {@code NOT_SHARING}
 * @param withdrawnAt      when they stopped sharing; null unless withdrawn
 * @param plotCount        their active plots; null unless sharing
 * @param registeredAreaHa the hectares of those plots; null unless sharing
 * @param sectorNames      the sectors of those plots; empty unless sharing
 * @param worstRiskLevel   the color of their most urgent plot; null unless sharing or without plots
 * @param worstReason      the first reason of that plot; null when it has none
 */
public record CooperativeMemberView(String id, String fullName, String sharing, Instant withdrawnAt,
                                    Integer plotCount, BigDecimal registeredAreaHa, List<String> sectorNames,
                                    String worstRiskLevel, RiskReasonView worstReason) {
}
