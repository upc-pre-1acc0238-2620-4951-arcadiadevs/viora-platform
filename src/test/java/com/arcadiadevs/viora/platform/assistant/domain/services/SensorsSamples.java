package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Coverage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Reading;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;

import java.time.LocalDate;
import java.util.List;

/** Sensors figures of a plot on 30 October 2026: its nodes, their latest reading and the readings of the week. */
final class SensorsSamples {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    private SensorsSamples() {
    }

    static SensorNode virtualMicroclimate() {
        return new SensorNode("MICROCLIMATE", null, true, null, null, null);
    }

    static SensorNode virtualProbe() {
        return new SensorNode("SOIL_PROBE", 30, true, 16.0, 20.0, null);
    }

    static SensorNode realMicroclimate() {
        return new SensorNode("MICROCLIMATE", null, false, null, null, null);
    }

    static SensorNode realProbe() {
        return new SensorNode("SOIL_PROBE", 30, false, 16.0, 20.0, null);
    }

    /** 27.4 °C, 38 % air humidity and 21.5 % in the soil. */
    static Reading reading() {
        return new Reading(27.4, 38.0, 21.5);
    }

    /** Readings on {@code days} of the last 7 days, the latest today. */
    static Coverage coverage(int days) {
        return new Coverage(days == 0 ? null : TODAY, days, 7);
    }

    static SensorsFacts sensors(List<SensorNode> nodes, Reading reading, Coverage coverage) {
        return new SensorsFacts(nodes, reading, coverage);
    }
}
