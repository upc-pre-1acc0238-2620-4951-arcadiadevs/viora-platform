package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.TreeSampleItem;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SubmitSamplingResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.TreeSampleResourceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * REST assembler converting {@link SubmitSamplingResource} payload into domain {@link IngestFieldSamplingsBatchCommand}.
 */
public final class IngestFieldSamplingsBatchCommandFromResourceAssembler {

    private IngestFieldSamplingsBatchCommandFromResourceAssembler() {
    }

    /**
     * Converts the HTTP request body resource and route plotId to domain command.
     *
     * @param plotId   the plot UUID string
     * @param resource the request body resource
     * @return consistent IngestFieldSamplingsBatchCommand
     */
    public static IngestFieldSamplingsBatchCommand toCommandFromResource(String plotId, SubmitSamplingResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("thinning.command.null");
        }
        List<TreeSampleItem> sampleItems = new ArrayList<>();
        if (resource.samples() != null) {
            for (TreeSampleResourceItem item : resource.samples()) {
                sampleItems.add(new TreeSampleItem(
                        item.treeTag(),
                        item.shootCount(),
                        item.fruitSetCount(),
                        item.trunkDiameterMm(),
                        item.samplingDate()
                ));
            }
        }
        return new IngestFieldSamplingsBatchCommand(
                plotId,
                resource.actorId(),
                resource.campaignYear(),
                resource.clientBatchId(),
                sampleItems
        );
    }
}
