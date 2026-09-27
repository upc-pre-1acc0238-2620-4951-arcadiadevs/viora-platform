package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices.IoTDeviceQueryServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDevicesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IoTDeviceQueryService Unit Tests")
class IoTDeviceQueryServiceTest {

    @Mock
    private IoTDeviceRepository ioTDeviceRepository;

    private IoTDeviceQueryServiceImpl queryService;

    private final String validPlotId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        queryService = new IoTDeviceQueryServiceImpl(ioTDeviceRepository);
    }

    @Test
    @DisplayName("Should retrieve all devices bound to a plot successfully")
    void shouldReturnDevicesWhenQueryIsValid() {
        var plotIdVo = new PlotId(validPlotId);
        var device = IoTDevice.register(
                plotIdVo,
                new DeviceName("Soil Moisture Sensor 1"),
                DeviceType.SOIL_PROBE,
                new SensorDepth(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceRepository.findAllByPlotId(plotIdVo)).thenReturn(List.of(device));

        var query = new GetIoTDevicesByPlotIdQuery(validPlotId);
        var result = queryService.handle(query);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().snapshot().name().value()).isEqualTo("Soil Moisture Sensor 1");
        verify(ioTDeviceRepository).findAllByPlotId(plotIdVo);
    }

    @Test
    @DisplayName("Should enforce constructor dependency null guards")
    @SuppressWarnings("DataFlowIssue")
    void shouldEnforceConstructorNullGuards() {
        assertThatThrownBy(() -> new IoTDeviceQueryServiceImpl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.repository.null");
    }

    @Test
    @DisplayName("Should enforce null query guard")
    @SuppressWarnings("DataFlowIssue")
    void shouldEnforceNullQueryGuard() {
        assertThatThrownBy(() -> queryService.handle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query.null");
    }

    @Test
    @DisplayName("Should validate GetIoTDevicesByPlotIdQuery non-null constraints")
    @SuppressWarnings("DataFlowIssue")
    void shouldValidateQueryNonNullConstraints() {
        assertThatThrownBy(() -> new GetIoTDevicesByPlotIdQuery((PlotId) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.plot_id.null_or_empty");

        assertThatThrownBy(() -> new GetIoTDevicesByPlotIdQuery((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.null_or_empty");

        assertThatThrownBy(() -> new GetIoTDevicesByPlotIdQuery("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.null_or_empty");
    }
}
