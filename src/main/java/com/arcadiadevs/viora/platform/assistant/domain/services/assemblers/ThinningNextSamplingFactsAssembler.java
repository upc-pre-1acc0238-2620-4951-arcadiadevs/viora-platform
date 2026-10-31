package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuándo vuelvo a muestrear?": with the thinning recorded this campaign needs no more sampling; the next one is
 * sampled after recording its full bloom, before the window the profile of the variety opens that many days after
 * it. Thinning has no other rule for the next sampling, so the answer gives no date.
 */
public final class ThinningNextSamplingFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.THINNING_NEXT_SAMPLING;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var outcome = home.outcome();
        if (outcome == null) {
            return Optional.empty();
        }
        int nextCampaign = data.campaignYear() + 1;
        facts.put("nextCampaignYear", nextCampaign);
        facts.put("thinningRecordedOn", outcome.executedOn().toString());
        final CatalogText text;
        final FactCardTemplate windowCard;
        if (outcome.windowOpensDaysAfterBloom() != null && outcome.windowClosesDaysAfterBloom() != null) {
            int opens = outcome.windowOpensDaysAfterBloom();
            int closes = outcome.windowClosesDaysAfterBloom();
            facts.put("windowOpensDaysAfterBloom", opens);
            facts.put("windowClosesDaysAfterBloom", closes);
            text = text(questionId(), "text.window", TextDate.of(outcome.executedOn()), String.valueOf(nextCampaign),
                    opens, closes);
            windowCard = card(shared("value.integer_range", opens, closes), shared("card.days_after_bloom.label"),
                    shared("card.window.label"), FactTone.HIGHLIGHT);
        } else {
            text = text(questionId(), "text", TextDate.of(outcome.executedOn()), String.valueOf(nextCampaign));
            windowCard = card(shared("card.done.value"), shared("card.done.label"),
                    shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("card.date.value", String.valueOf(nextCampaign)), shared("card.next_campaign.label"),
                        shared("card.after_bloom.pill"), FactTone.PRIMARY),
                windowCard), List.of(shared("source.thinning", TextDate.of(outcome.executedOn()))), List.of()));
    }
}
