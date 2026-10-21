package com.arcadiadevs.viora.platform.subscription.application.ports;

/** Provider notification authentication port; concrete cryptography stays in infrastructure. */
public interface PaymentNotificationAuthenticator {
    /** @return whether a signing secret is configured */
    boolean configured();

    /**
     * Validates the provider's signed notification envelope.
     * @param dataId canonical payment identifier
     * @param requestId provider request identifier
     * @param signature signed header
     * @return whether authentication succeeds
     */
    boolean valid(String dataId, String requestId, String signature);
}
