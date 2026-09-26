package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.adapters.IoTDeviceRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("IoTDevice JPA Persistence and Adapter Integration Tests")
class IoTDevicePersistenceIntegrationTest {

    @Autowired
    private IoTDeviceRepositoryImpl ioTDeviceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PlotId plotId = new PlotId();

    @Test
    @DisplayName("Should successfully persist and reconstitute IoTDevice aggregate through JPA adapter")
    void shouldPersistAndReconstituteIoTDevice() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Edafica Sector Sur"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(45),
                SoilTextureType.CLAY_LOAM,
                new CalibrationMultiplier(1.10)
        );

        var saved = ioTDeviceRepository.save(device);
        entityManager.flush();
        entityManager.clear();

        var reloadedOpt = ioTDeviceRepository.findById(saved.snapshot().id());

        assertThat(reloadedOpt).isPresent();
        var reloaded = reloadedOpt.get().snapshot();
        assertThat(reloaded.id()).isEqualTo(saved.snapshot().id());
        assertThat(reloaded.plotId()).isEqualTo(plotId);
        assertThat(reloaded.name().value()).isEqualTo("Sonda Edafica Sector Sur");
        assertThat(reloaded.type()).isEqualTo(DeviceType.SOIL_PROBE);
        assertThat(reloaded.depth().depthCm()).isEqualTo(45);
        assertThat(reloaded.soilTextureType()).isEqualTo(SoilTextureType.CLAY_LOAM);
        assertThat(reloaded.calibrationMultiplier().value()).isEqualTo(1.10);
        assertThat(reloaded.status()).isEqualTo(DeviceStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should verify existsByNameAndPlotId accurately")
    void shouldVerifyExistsByNameAndPlotId() {
        var deviceName = new DeviceName("Estacion Norte");
        var device = IoTDevice.register(
                plotId,
                deviceName,
                DeviceType.MICROCLIMATE,
                SensorDepth.none(),
                SoilTextureType.NOT_APPLICABLE,
                null
        );

        ioTDeviceRepository.save(device);
        entityManager.flush();

        assertThat(ioTDeviceRepository.existsByNameAndPlotId(deviceName, plotId)).isTrue();
        assertThat(ioTDeviceRepository.existsByNameAndPlotId(new DeviceName("Nombre Inexistente"), plotId)).isFalse();
    }

    @Test
    @DisplayName("Should update existing device entity and increment revision")
    void shouldUpdateExistingDeviceEntity() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Este"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        var saved = ioTDeviceRepository.save(device);
        entityManager.flush();

        saved.calibrate(SensorDepth.of(60), SoilTextureType.CLAY, new CalibrationMultiplier(1.25));
        var updated = ioTDeviceRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        var reloadedOpt = ioTDeviceRepository.findById(updated.snapshot().id());
        assertThat(reloadedOpt).isPresent();
        var reloaded = reloadedOpt.get().snapshot();
        assertThat(reloaded.depth().depthCm()).isEqualTo(60);
        assertThat(reloaded.soilTextureType()).isEqualTo(SoilTextureType.CLAY);
        assertThat(reloaded.calibrationMultiplier().value()).isEqualTo(1.25);
    }
}
