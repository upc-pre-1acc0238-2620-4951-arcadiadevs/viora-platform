package com.arcadiadevs.viora.platform.territory.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.RenameSectorZoneCommand;

/**
 * Command service port of the sector list a manager keeps for their license.
 */
public interface SectorZoneCommandService {

    /**
     * Adds a sector to the manager's license.
     *
     * @param command the manager and the name
     * @return the new sector, or why it was not added
     */
    Result<SectorZone, ApplicationError> handle(CreateSectorZoneCommand command);

    /**
     * Renames a sector of the manager's license; every plot in it shows the new name.
     *
     * @param command the manager, the sector and the new name
     * @return the renamed sector, or why it was not renamed
     */
    Result<SectorZone, ApplicationError> handle(RenameSectorZoneCommand command);
}
