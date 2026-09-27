package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.telemetry.application.queryservices.IoTDeviceQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDevicesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link IoTDeviceQueryService} handling IoT device queries without business side-effects.
 */
@Service
@Transactional(readOnly = true)
public class IoTDeviceQueryServiceImpl implements IoTDeviceQueryService {

    private final IoTDeviceRepository ioTDeviceRepository;

    /**
     * Constructs the IoTDeviceQueryServiceImpl with required dependencies.
     *
     * @param ioTDeviceRepository the domain device repository port
     */
    public IoTDeviceQueryServiceImpl(IoTDeviceRepository ioTDeviceRepository) {
        if (ioTDeviceRepository == null) {
            throw new IllegalArgumentException("device.repository.null");
        }
        this.ioTDeviceRepository = ioTDeviceRepository;
    }

    @Override
    public List<IoTDevice> handle(GetIoTDevicesByPlotIdQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query.null");
        }
        return ioTDeviceRepository.findAllByPlotId(query.plotId());
    }
}
