package com.arcadiadevs.viora.platform.shared.domain.model.valueobjects;

import java.time.ZoneId;

/**
 * The time zone of the producers' calendar: Peru, which keeps UTC-5 all year.
 *
 * <p>Instants are absolute and stay in UTC. Every calendar date the platform derives (today, the campaign year, the
 * day of a reading) uses this zone, so from 19:00 to midnight in Peru "today" is still today and not tomorrow.</p>
 */
public final class ProducerTimeZone {

    /** America/Lima. */
    public static final ZoneId ZONE = ZoneId.of("America/Lima");

    private ProducerTimeZone() {
    }
}
