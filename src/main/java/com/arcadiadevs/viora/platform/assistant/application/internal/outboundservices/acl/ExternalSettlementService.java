package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts.SettledCaliber;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.interfaces.acl.SettlementContextFacade;
import org.springframework.stereotype.Service;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Outbound ACL service used by the assistant to read the caliber the mill weighed for each settled campaign of a plot.
 */
@Service("assistantExternalSettlementService")
public class ExternalSettlementService {

    private final SettlementContextFacade settlementContextFacade;

    public ExternalSettlementService(SettlementContextFacade settlementContextFacade) {
        this.settlementContextFacade = settlementContextFacade;
    }

    /**
     * The settled caliber of each campaign of the plot.
     *
     * @param plotId the assistant PlotId value object
     * @return the calibers by campaign year; empty when no campaign of the plot was settled with a caliber
     */
    public SortedMap<Integer, SettledCaliber> findSettledCalibers(PlotId plotId) {
        var calibers = new TreeMap<Integer, SettledCaliber>();
        for (var caliber : settlementContextFacade.findSettledCalibers(plotId.plotId())) {
            calibers.put(caliber.campaignYear(),
                    new SettledCaliber(caliber.fruitsPerKg(), caliber.commercialSizeGrade()));
        }
        return calibers;
    }
}
