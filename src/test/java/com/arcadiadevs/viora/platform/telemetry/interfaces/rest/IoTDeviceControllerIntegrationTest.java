package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.IoTDeviceCommandService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers.IoTDeviceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("IoTDeviceController REST Endpoint Integration Tests")
class IoTDeviceControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private IoTDeviceCommandService ioTDeviceCommandService;

    @Mock
    private IoTDeviceRepository ioTDeviceRepository;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new IoTDeviceController(ioTDeviceCommandService, ioTDeviceRepository);
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
}
