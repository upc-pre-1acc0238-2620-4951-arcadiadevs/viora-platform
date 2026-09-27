package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.IoTDeviceCommandService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CalibrateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.DeactivateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers.IoTDeviceController;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.IoTDeviceQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDevicesByPlotIdQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("IoTDeviceController REST Endpoint Integration Tests")
class IoTDeviceControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private IoTDeviceCommandService ioTDeviceCommandService;

    @Mock
    private IoTDeviceQueryService ioTDeviceQueryService;

    @Mock
    private IoTDeviceRepository ioTDeviceRepository;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new IoTDeviceController(
                ioTDeviceCommandService,
                ioTDeviceQueryService,
                ioTDeviceRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/iot-devices should return 201 Created on valid input")
    void shouldReturnCreatedWhenPayloadIsValid() throws Exception {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda Edafica Sector Norte"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.SANDY_LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceCommandService.handle(any(RegisterIoTDeviceCommand.class)))
                .thenReturn(Result.success(device.snapshot().id().deviceId()));
        when(ioTDeviceRepository.findById(eq(device.snapshot().id())))
                .thenReturn(Optional.of(device));

        String payload = """
                {
                    "name": "Sonda Edafica Sector Norte",
                    "type": "SOIL_PROBE",
                    "depthCm": 30,
                    "soilTextureType": "SANDY_LOAM",
                    "calibrationMultiplier": 1.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(device.snapshot().id().deviceId())))
                .andExpect(jsonPath("$.plotId", is(plotId.toString())))
                .andExpect(jsonPath("$.name", is("Sonda Edafica Sector Norte")))
                .andExpect(jsonPath("$.type", is("SOIL_PROBE")))
                .andExpect(jsonPath("$.depthCm", is(30)))
                .andExpect(jsonPath("$.soilTextureType", is("SANDY_LOAM")))
                .andExpect(jsonPath("$.calibrationMultiplier", is(1.0)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/iot-devices should return 400 Bad Request when name is blank")
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        String payload = """
                {
                    "name": "",
                    "type": "SOIL_PROBE",
                    "depthCm": 30
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/iot-devices should return 409 Conflict when name already exists")
    void shouldReturnConflictWhenDeviceNameExists() throws Exception {
        when(ioTDeviceCommandService.handle(any(RegisterIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("device", "device.name.duplicate")));

        String payload = """
                {
                    "name": "Sonda Duplicada",
                    "type": "MICROCLIMATE"
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/iot-devices should return 200 OK with device list")
    void shouldReturnOkWithDeviceList() throws Exception {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda Edafica Sector Norte"),
                DeviceType.SOIL_PROBE,
                new SensorDepth(30),
                SoilTextureType.SANDY_LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceQueryService.handle(any(GetIoTDevicesByPlotIdQuery.class)))
                .thenReturn(List.of(device));

        mockMvc.perform(get("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Sonda Edafica Sector Norte")))
                .andExpect(jsonPath("$[0].type", is("SOIL_PROBE")))
                .andExpect(jsonPath("$[0].depthCm", is(30)))
                .andExpect(jsonPath("$[0].soilTextureType", is("SANDY_LOAM")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/iot-devices should return 200 OK with empty list when no devices exist")
    void shouldReturnOkWithEmptyListWhenNoDevices() throws Exception {
        when(ioTDeviceQueryService.handle(any(GetIoTDevicesByPlotIdQuery.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 200 OK on valid calibration")
    void shouldReturnOkOnValidCalibration() throws Exception {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda a Calibrar"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );
        var deviceId = device.snapshot().id().deviceId();

        when(ioTDeviceCommandService.handle(any(CalibrateIoTDeviceCommand.class)))
                .thenReturn(Result.success(deviceId));
        when(ioTDeviceRepository.findById(eq(new DeviceId(deviceId))))
                .thenReturn(Optional.of(device));

        String payload = """
                {
                    "calibrationMultiplier": 1.15,
                    "soilTextureType": "CLAY_LOAM",
                    "depthCm": 40
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"0\"")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(deviceId)))
                .andExpect(jsonPath("$.plotId", is(plotId.toString())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 400 Bad Request when multiplier out of range")
    void shouldReturnBadRequestWhenMultiplierOutOfRange() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        String payload = """
                {
                    "calibrationMultiplier": 0.20,
                    "soilTextureType": "LOAM",
                    "depthCm": 30
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 400 Bad Request when If-Match header is non-numeric")
    void shouldReturnBadRequestWhenIfMatchHeaderIsInvalid() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        String payload = """
                {
                    "calibrationMultiplier": 1.10
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "not-a-number")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 404 Not Found when device does not exist")
    void shouldReturnNotFoundWhenCalibratingMissingDevice() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(CalibrateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("IoTDevice", deviceId)));

        String payload = """
                {
                    "calibrationMultiplier": 1.10
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 409 Conflict when device plot mismatch")
    void shouldReturnConflictWhenPlotMismatchOnCalibration() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(CalibrateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("device", "device.plot_id.mismatch")));

        String payload = """
                {
                    "calibrationMultiplier": 1.10
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 412 Precondition Failed on revision mismatch")
    void shouldReturnPreconditionFailedOnRevisionMismatch() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(CalibrateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.preconditionFailed("device", "device.revision.mismatch")));

        String payload = """
                {
                    "calibrationMultiplier": 1.10
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"5\"")
                        .content(payload))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 422 Unprocessable Content when device not active")
    void shouldReturnUnprocessableEntityWhenDeviceNotActive() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(CalibrateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.businessRuleViolation("DeviceNotActive", "device.status.not_active")));

        String payload = """
                {
                    "calibrationMultiplier": 1.10
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 200 OK on valid deactivation")
    void shouldReturnOkOnValidDeactivation() throws Exception {
        var deviceId = UUID.randomUUID().toString();

        when(ioTDeviceCommandService.handle(any(DeactivateIoTDeviceCommand.class)))
                .thenReturn(Result.success(deviceId));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("IoT device unlinked successfully")));
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 400 Bad Request when If-Match header is non-numeric")
    void shouldReturnBadRequestWhenIfMatchHeaderIsInvalidOnDeactivate() throws Exception {
        var deviceId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .header("If-Match", "non-numeric"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 404 Not Found when device does not exist")
    void shouldReturnNotFoundWhenDeactivatingMissingDevice() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(DeactivateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("IoTDevice", deviceId)));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 409 Conflict when device plot mismatch")
    void shouldReturnConflictWhenPlotMismatchOnDeactivate() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(DeactivateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("device", "device.plot_id.mismatch")));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 409 Conflict when device is already unlinked")
    void shouldReturnConflictWhenDeviceAlreadyUnlinked() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(DeactivateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("device", "device.already_unlinked")));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId} should return 412 Precondition Failed on revision mismatch")
    void shouldReturnPreconditionFailedOnRevisionMismatchOnDeactivate() throws Exception {
        var deviceId = UUID.randomUUID().toString();
        when(ioTDeviceCommandService.handle(any(DeactivateIoTDeviceCommand.class)))
                .thenReturn(Result.failure(ApplicationError.preconditionFailed("device", "device.revision.mismatch")));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .header("If-Match", "\"5\""))
                .andExpect(status().isPreconditionFailed());
    }
}
