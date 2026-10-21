package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.RedeemCooperativeCodeResource;

/**
 * Assembler from the redemption request to its command.
 */
public final class RedeemCooperativeCodeCommandFromResourceAssembler {

    private RedeemCooperativeCodeCommandFromResourceAssembler() {
    }

    /**
     * Builds the redemption command for the authenticated producer.
     *
     * @param producerId the authenticated producer
     * @param resource   the request payload
     * @return the command
     */
    public static RedeemCooperativeCodeCommand toCommandFromResource(String producerId,
                                                                     RedeemCooperativeCodeResource resource) {
        return new RedeemCooperativeCodeCommand(producerId, resource.code());
    }
}
