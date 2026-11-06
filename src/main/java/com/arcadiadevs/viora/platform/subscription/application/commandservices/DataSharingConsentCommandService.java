package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.WithdrawDataSharingConsentCommand;

/**
 * Command service port of the data sharing consents producers give when redeeming a cooperative code.
 */
public interface DataSharingConsentCommandService {

    /**
     * Stops sharing a producer's data for good. The plan and its seat stay.
     *
     * @param command the withdrawal request
     * @return the withdrawn consent, or the reason it was not withdrawn
     */
    Result<DataSharingConsent, ApplicationError> handle(WithdrawDataSharingConsentCommand command);
}
