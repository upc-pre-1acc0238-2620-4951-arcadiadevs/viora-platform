package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read model of one member as the manager reads them. Contact and plots only while they share.
 *
 * @param id               the member, their user identifier
 * @param fullName         their name
 * @param sharing          {@code SHARING}, {@code WITHDRAWN} or {@code NOT_SHARING}
 * @param withdrawnAt      when they stopped sharing; null unless withdrawn
 * @param memberSince      when their sponsored plan started
 * @param quotaHa          the hectares their code covers
 * @param maskedCode       the code they redeemed, masked
 * @param phone            their phone; null unless sharing
 * @param email            their email; null unless sharing
 * @param registeredAreaHa the hectares of their active plots; null unless sharing
 * @param plots            their plots with sector and color; empty unless sharing
 */
public record CooperativeMemberDetailView(String id, String fullName, String sharing, Instant withdrawnAt,
                                          Instant memberSince, BigDecimal quotaHa, String maskedCode, String phone,
                                          String email, BigDecimal registeredAreaHa, List<MemberPlotView> plots) {
}
