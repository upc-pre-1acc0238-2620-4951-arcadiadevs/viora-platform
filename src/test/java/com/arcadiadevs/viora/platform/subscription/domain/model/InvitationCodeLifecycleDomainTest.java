package com.arcadiadevs.viora.platform.subscription.domain.model;

import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseAreaException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseSeatsException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRevocableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeFilter;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invitation code lifecycle domain unit tests")
class InvitationCodeLifecycleDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-12-31T05:00:00Z");
    private static final Instant IN_30_DAYS = NOW.plus(Duration.ofDays(30));

    private final CooperativeLicense license = newLicense(60, "200");

    @Test
    @DisplayName("A batch holds a seat and the hectares of each code, all in one batch, readable and masked")
    void issuingABatch() {
        var codes = license.issueCodes(sealed("COOP-4F9T", "COOP-9HX3"), quota("5"), 44, NOW, IN_30_DAYS);

        assertThat(codes).hasSize(2).allSatisfy(code -> {
            var snapshot = code.snapshot();
            assertThat(snapshot.status()).isEqualTo(InvitationCodeStatus.AVAILABLE);
            assertThat(snapshot.quota().hectares()).isEqualByComparingTo("5");
            assertThat(snapshot.issuedAt()).isEqualTo(NOW);
            assertThat(snapshot.expiresAt()).isEqualTo(IN_30_DAYS);
            assertThat(snapshot.sealedCode()).startsWith("sealed:");
            assertThat(snapshot.batchId()).isNotNull();
        });
        assertThat(codes.get(0).snapshot().batchId()).isEqualTo(codes.get(1).snapshot().batchId());
        assertThat(codes.get(0).snapshot().hint()).isEqualTo("9T");
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("A batch that needs more seats or hectares than are free changes nothing")
    void notEnoughSeatsOrHectares() {
        assertThatThrownBy(() -> license.issueCodes(sealed(13), quota("1"), 48, NOW, IN_30_DAYS))
                .isInstanceOf(InsufficientLicenseSeatsException.class);
        assertThatThrownBy(() -> license.issueCodes(sealed(4), quota("50.5"), 48, NOW, IN_30_DAYS))
                .isInstanceOf(InsufficientLicenseAreaException.class);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("0");

        assertThat(license.issueCodes(sealed(12), quota("1"), 48, NOW, IN_30_DAYS)).hasSize(12);
    }

    @Test
    @DisplayName("Codes are issued only while the license is in force, and never outlive it")
    void codesNeverOutliveTheLicense() {
        assertThatThrownBy(() -> license.issueCodes(sealed(1), quota("1"), 0, NOW, LICENSE_END.plusSeconds(1)))
                .hasMessage("subscription.invitation_code.expires_after_license");
        var ended = CooperativeLicense.grant(new CooperativeId(), "Coop. Anterior", 10,
                new HectaresQuota(new BigDecimal("50")), new SubscriptionPeriod(NOW.minus(Duration.ofDays(400)), NOW));
        assertThatThrownBy(() -> ended.issueCodes(sealed(1), quota("1"), 0, NOW, NOW.plusSeconds(60)))
                .hasMessage("subscription.license.not_in_force");
        assertThat(license.issueCodes(sealed(1), quota("1"), 0, NOW, LICENSE_END)).hasSize(1);
    }

    @Test
    @DisplayName("Revoking a code nobody redeemed gives its hectares back and forgets the code")
    void revoking() {
        var code = license.issueCodes(sealed(1), quota("5"), 0, NOW, IN_30_DAYS).getFirst();

        license.revokeCode(code, NOW.plusSeconds(60));

        assertThat(code.snapshot().status()).isEqualTo(InvitationCodeStatus.REVOKED);
        assertThat(code.snapshot().sealedCode()).isNull();
        assertThat(code.snapshot().hint()).isNotNull();
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> license.revokeCode(code, NOW.plusSeconds(120)))
                .isInstanceOf(InvitationCodeNotRevocableException.class)
                .hasMessage(InvitationCodeNotRevocableException.NOT_AVAILABLE_KEY);
    }

    @Test
    @DisplayName("A redeemed code cannot be revoked, says when it was redeemed, and keeps its hectares")
    void aRedeemedCodeStays() {
        var code = license.issueCodes(sealed(1), quota("5"), 0, NOW, IN_30_DAYS).getFirst();
        var redeemedAt = NOW.plusSeconds(30);
        license.redeem(code, new ProducerId(), redeemedAt);
        assertThat(code.snapshot().sealedCode()).isNull();

        assertThatThrownBy(() -> license.revokeCode(code, NOW.plusSeconds(60)))
                .isInstanceOfSatisfying(InvitationCodeNotRevocableException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo(InvitationCodeNotRevocableException.ALREADY_REDEEMED_KEY);
                    assertThat(ex.redeemedAt()).isEqualTo(redeemedAt);
                });
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("A code whose time is over shows as expired, and gives its hectares back once, when marked")
    void expiring() {
        var code = license.issueCodes(sealed(1), quota("5"), 0, NOW, IN_30_DAYS).getFirst();

        assertThat(license.expireCode(code, IN_30_DAYS.minusSeconds(1))).isFalse();
        assertThat(code.statusAt(IN_30_DAYS)).isEqualTo(InvitationCodeStatus.EXPIRED);
        assertThat(code.isOverdue(IN_30_DAYS)).isTrue();
        assertThat(InvitationCodeFilter.CLOSED.includes(code.statusAt(IN_30_DAYS))).isTrue();

        assertThat(license.expireCode(code, IN_30_DAYS)).isTrue();
        assertThat(license.expireCode(code, IN_30_DAYS)).isFalse();
        assertThat(code.snapshot().sealedCode()).isNull();
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> license.revokeCode(code, IN_30_DAYS))
                .hasMessage(InvitationCodeNotRevocableException.NOT_AVAILABLE_KEY);
    }

    @Test
    @DisplayName("The capacity frees overdue hectares and suggests the free hectares per free seat, by half hectares")
    void capacity() {
        license.issueCodes(sealed(46), quota("3"), 0, NOW, IN_30_DAYS);

        var capacity = license.capacity(48, new BigDecimal("0"));
        assertThat(capacity.seatsUsed()).isEqualTo(48);
        assertThat(capacity.seatsFree()).isEqualTo(12);
        assertThat(capacity.reservedAreaHa()).isEqualByComparingTo("138");
        assertThat(capacity.freeAreaHa()).isEqualByComparingTo("62");
        assertThat(capacity.suggestedHectaresPerCode()).isEqualByComparingTo("5.0");

        var withOverdue = license.capacity(48, new BigDecimal("6"));
        assertThat(withOverdue.freeAreaHa()).isEqualByComparingTo("68");
        assertThat(withOverdue.suggestedHectaresPerCode()).isEqualByComparingTo("5.5");

        var full = license.capacity(60, BigDecimal.ZERO);
        assertThat(full.seatsFree()).isZero();
        assertThat(full.suggestedHectaresPerCode()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("A code of another license is refused")
    void codeOfAnotherLicense() {
        var code = license.issueCodes(sealed(1), quota("5"), 0, NOW, IN_30_DAYS).getFirst();
        var other = newLicense(10, "50");

        assertThatThrownBy(() -> other.revokeCode(code, NOW))
                .hasMessage("subscription.license.code_of_another_license");
        assertThatThrownBy(() -> other.expireCode(code, IN_30_DAYS))
                .hasMessage("subscription.license.code_of_another_license");
    }

    private static CooperativeLicense newLicense(int seats, String hectares) {
        return CooperativeLicense.grant(new CooperativeId(), "Cooperativa Olivar del Sur", seats,
                new HectaresQuota(new BigDecimal(hectares)),
                new SubscriptionPeriod(NOW.minus(Duration.ofDays(1)), LICENSE_END));
    }

    private static HectaresQuota quota(String hectares) {
        return new HectaresQuota(new BigDecimal(hectares));
    }

    private static List<InvitationCode.IssuedCode> sealed(String... codes) {
        return List.of(codes).stream()
                .map(code -> new InvitationCode.IssuedCode(new CooperativeCode(code).fingerprint(),
                        code.substring(code.length() - 2), "sealed:" + code))
                .toList();
    }

    private static List<InvitationCode.IssuedCode> sealed(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "COOP-%04d".formatted(index).replace('0', 'A').replace('1', 'B'))
                .map(code -> new InvitationCode.IssuedCode(new CooperativeCode(code).fingerprint(),
                        code.substring(code.length() - 2), "sealed:" + code))
                .toList();
    }
}
