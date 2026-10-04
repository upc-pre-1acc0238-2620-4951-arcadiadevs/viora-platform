package com.arcadiadevs.viora.platform.telemetry.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CompleteMitigationStepCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.NormalizeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.PostponeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;

/**
 * Application command service port orchestrating state mutation use cases for Agroclimatic Incidents.
 */
public interface AgroclimaticIncidentCommandService {

    /**
     * Raises a new agroclimatic incident if thresholds are breached.
     *
     * @param command the command specification
     * @return Result containing the created incident ID or error
     */
    Result<String, ApplicationError> handle(RaiseAgroclimaticIncidentCommand command);

    /**
     * Normalizes and resolves an active incident.
     *
     * @param command the normalization command
     * @return Result containing the incident ID or error
     */
    Result<String, ApplicationError> handle(NormalizeAgroclimaticIncidentCommand command);

    /**
     * Postpones (snoozes) notifications for an active incident.
     *
     * @param command the postponement command
     * @return Result containing the incident ID or error
     */
    Result<String, ApplicationError> handle(PostponeAgroclimaticIncidentCommand command);

    /**
     * Completes an actionable mitigation step within an incident.
     *
     * @param command the completion command
     * @return Result containing the incident ID or error
     */
    Result<String, ApplicationError> handle(CompleteMitigationStepCommand command);
}
