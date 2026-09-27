package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.CalibrateIoTDeviceResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalibrateIoTDeviceCommandFromResourceAssembler Unit Tests")
class CalibrateIoTDeviceCommandFromResourceAssemblerTest {

    @Test
    @DisplayName("Should transform resource and route parameters to command correctly")
    void shouldTransformResourceToCommand() {
        var plotId = UUID.randomUUID().toString();
        var deviceId = UUID.randomUUID().toString();
        var resource = new CalibrateIoTDeviceResource(1.25, "SANDY_LOAM", 45);

        var command = CalibrateIoTDeviceCommandFromResourceAssembler.toCommandFromResource(
                plotId, deviceId, 1L, resource
        );

        assertThat(command.plotId()).isEqualTo(plotId);
        assertThat(command.deviceId()).isEqualTo(deviceId);
        assertThat(command.calibrationMultiplier()).isEqualTo(1.25);
        assertThat(command.soilTextureType()).isEqualTo("SANDY_LOAM");
        assertThat(command.depthCm()).isEqualTo(45);
        assertThat(command.expectedRevision()).isEqualTo(1L);
    }
}
