package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IoTDeviceResourceFromEntityAssembler Unit Tests")
class IoTDeviceResourceFromEntityAssemblerTest {

    @Test
    @DisplayName("Should transform IoTDevice entity to IoTDeviceResource with all attributes")
    void shouldTransformEntityToResource() {
        var plotId = new PlotId();
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Central"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(60),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        var resource = IoTDeviceResourceFromEntityAssembler.toResourceFromEntity(device);

        assertThat(resource).isNotNull();
        assertThat(resource.id()).isEqualTo(device.snapshot().id().deviceId());
        assertThat(resource.plotId()).isEqualTo(plotId.plotId());
        assertThat(resource.name()).isEqualTo("Sonda Central");
        assertThat(resource.type()).isEqualTo("SOIL_PROBE");
        assertThat(resource.depthCm()).isEqualTo(60);
        assertThat(resource.soilTextureType()).isEqualTo("LOAM");
        assertThat(resource.calibrationMultiplier()).isEqualTo(1.0);
        assertThat(resource.status()).isEqualTo("ACTIVE");
        assertThat(resource.revision()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should transform list of IoTDevice entities to list of IoTDeviceResources")
    void shouldTransformListToResources() {
        var plotId = new PlotId();
        var device1 = IoTDevice.register(plotId, new DeviceName("Sonda 1"), DeviceType.SOIL_PROBE, SensorDepth.of(30), SoilTextureType.LOAM, null);
        var device2 = IoTDevice.register(plotId, new DeviceName("Estacion 1"), DeviceType.MICROCLIMATE, SensorDepth.none(), SoilTextureType.NOT_APPLICABLE, null);

        var list = IoTDeviceResourceFromEntityAssembler.toResourceList(List.of(device1, device2));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).name()).isEqualTo("Sonda 1");
        assertThat(list.get(1).name()).isEqualTo("Estacion 1");
    }
}
