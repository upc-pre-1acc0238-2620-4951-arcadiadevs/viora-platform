package com.arcadiadevs.viora.platform.subscription.application.ports;

/** Narrow cross-context readiness check. */
public interface ProducerProfilePort {
    boolean isReady(String producerId);
}
