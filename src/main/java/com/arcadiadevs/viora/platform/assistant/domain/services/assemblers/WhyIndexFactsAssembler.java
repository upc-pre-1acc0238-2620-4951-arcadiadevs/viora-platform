package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Por qué mi índice es 0,51?": the Biennial Bearing Index, its band and the campaigns it comes from.
 */
public final class WhyIndexFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALTERNATION_WHY_INDEX;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        if (data.bearingIndex() == null || data.bearingCategory() == null || data.historicalYields().isEmpty()) {
            return Optional.empty();
        }
        double index = twoDecimals(data.bearingIndex());
        int campaigns = data.historicalYields().size();
        int lastYear = data.historicalYields().lastKey();
        long lastKg = whole(data.historicalYields().get(lastYear));
        var band = shared("band." + data.bearingCategory());

        var facts = new TreeMap<String, Object>();
        facts.put("bearingIndex", index);
        facts.put("bearingBand", data.bearingCategory());
        facts.put("harvestCampaigns", campaigns);
        facts.put("lastCampaignYear", lastYear);
        facts.put("lastCampaignKg", lastKg);
        return Optional.of(new AssembledFacts(facts,
                text(questionId(), "text", index, band, campaigns),
                List.of(
                        card(shared("value.decimal", index), shared("card.bbi.label"), band, FactTone.PRIMARY),
                        card(shared("value.kg", lastKg), shared("card.harvest.label", String.valueOf(lastYear)),
                                shared("card.campaigns.pill", campaigns), FactTone.HIGHLIGHT)),
                List.of(shared("source.campaigns", campaigns)),
                List.of()));
    }
}
