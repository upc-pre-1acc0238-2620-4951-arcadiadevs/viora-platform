package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.RegisterIoTDeviceResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisterIoTDeviceCommandFromResourceAssembler Unit Tests")
class RegisterIoTDeviceCommandFromResourceAssemblerTest {

    @Test
    @DisplayName("Should transform RegisterIoTDeviceResource and plotId into RegisterIoTDeviceCommand")
    void shouldTransformResourceToCommand() {
        var plotId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        var resource = new RegisterIoTDeviceResource(
                "Sonda Edafica Sector Norte",
                "SOIL_PROBE",
                30,
                "SANDY_LOAM",
                1.15
        );

        var command = RegisterIoTDeviceCommandFromResourceAssembler.toCommandFromResource(plotId, resource);

        assertThat(command).isNotNull();
        assertThat(command.plotId()).isEqualTo(plotId);
        assertThat(command.name()).isEqualTo("Sonda Edafica Sector Norte");
        assertThat(command.type()).isEqualTo("SOIL_PROBE");
        assertThat(command.depthCm()).isEqualTo(30);
        assertThat(command.soilTextureType()).isEqualTo("SANDY_LOAM");
        assertThat(command.calibrationMultiplier()).isEqualTo(1.15);
    }
}
