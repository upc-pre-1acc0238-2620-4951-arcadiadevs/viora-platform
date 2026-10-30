package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué hago esta semana?": three sentences, in this order: the thinning (an open window first, then one opening
 * this week, then a sampling left halfway), the hottest forecast day against the heat threshold, and the open
 * alerts. "This week" is today and the next six days. For the field, plots are counted, never named.
 */
public final class HomeWeekPlanFactsAssembler extends HomeFactsAssembler {

    /** Days of "this week", today included. */
    static final int WEEK_DAYS = 7;

    @Override
    public QuestionId questionId() {
        return QuestionId.HOME_WEEK_PLAN;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData home, SortedMap<String, Object> facts) {
        int active = home.activePlots();
        if (active == 0) {
            return Optional.empty();
        }
        var thinning = thinning(data.today(), home, facts);
        var forecast = hottest(home);
        var sources = new ArrayList<CatalogText>();
        final CatalogText weather;
        final FactCardTemplate weatherCard;
        if (forecast.isEmpty()) {
            weather = text(questionId(), "forecast.none");
            weatherCard = card(shared("card.no_forecast.value"), shared("card.week_max.label"),
                    shared("card.no_forecast.pill"), FactTone.PRIMARY);
        } else {
            var day = forecast.get().day();
            double max = oneDecimal(day.max());
            double threshold = oneDecimal(home.thresholds().heatWarning());
            facts.put("hottestMax", max);
            facts.put("hottestOn", day.date().toString());
            facts.put("heatThreshold", threshold);
            facts.put("heatReached", day.heatRisk());
            weather = text(questionId(), day.heatRisk() ? "forecast.heat" : "forecast.mild", max,
                    TextDate.of(day.date()), threshold);
            weatherCard = card(shared("value.measure", max, "°C"), shared("card.week_max.label"),
                    shared("card.on.pill", TextDate.shortOf(day.date())), FactTone.PRIMARY);
            sources.add(forecastSource(forecast.get().forecast()));
        }
        var alerts = home.openAlerts();
        int plotsWithAlerts = home.plotsWithAlerts();
        facts.put("openAlerts", alerts.size());
        facts.put("plotsWithAlerts", plotsWithAlerts);
        final CatalogText alertsText;
        var severe = ClimateData.mostSevere(alerts);
        if (severe.isEmpty()) {
            alertsText = text(questionId(), home.singlePlot() ? "alerts.none" : "alerts.none.field");
        } else {
            facts.put("incidentType", severe.get().type());
            facts.put("severity", severe.get().severity());
            alertsText = home.singlePlot()
                    ? text(questionId(), "alerts.some", alerts.size(), AlertFactsAssembler.alertName(severe.get()),
                            AlertFactsAssembler.severityName(severe.get()))
                    : text(questionId(), "alerts.some.field", alerts.size(),
                            AlertFactsAssembler.alertName(severe.get()),
                            AlertFactsAssembler.severityName(severe.get()), plotsWithAlerts);
        }
        var alertsCard = card(shared("value.integer", alerts.size()), shared("card.open_alerts.label", alerts.size()),
                home.singlePlot() ? shared("card.this_plot.pill") : shared("card.in_plots.pill", plotsWithAlerts),
                FactTone.HIGHLIGHT);
        if (home.plots().stream().anyMatch(plot -> plot.plan() != null)) {
            sources.add(shared("source.plans"));
        }
        sources.add(shared("source.open_alerts"));
        return Optional.of(new AssembledFacts(facts, text(questionId(), "text", thinning, weather, alertsText),
                List.of(weatherCard, alertsCard), sources, List.of()));
    }

    /** The thinning sentence: open window, window opening this week, sampling left halfway, or nothing pending. */
    private CatalogText thinning(LocalDate today, HomeData home, SortedMap<String, Object> facts) {
        boolean single = home.singlePlot();
        int active = home.activePlots();
        var weekEnd = today.plusDays(WEEK_DAYS - 1L);
        var issued = home.plots().stream().filter(plot -> plot.plan() != null && plot.plan().executedOn() == null
                && plot.plan().issued() && plot.plan().percentageToRemove() != null
                && plot.plan().windowOpensOn() != null && plot.plan().windowClosesOn() != null)
                .map(HomeData.PlotHome::plan)
                .toList();
        var open = issued.stream()
                .filter(plan -> !today.isBefore(plan.windowOpensOn()) && !today.isAfter(plan.windowClosesOn()))
                .sorted(Comparator.comparing(ThinningPlanData::windowClosesOn))
                .toList();
        var soon = issued.stream()
                .filter(plan -> today.isBefore(plan.windowOpensOn()) && !plan.windowOpensOn().isAfter(weekEnd))
                .sorted(Comparator.comparing(ThinningPlanData::windowOpensOn))
                .toList();
        var halfway = home.plots().stream().filter(plot -> plot.plan() != null && !plot.plan().issued()
                && plot.plan().sampledTrees() > 0 && plot.treesNeeded() > 0).toList();
        facts.put("plotsWindowOpen", open.size());
        facts.put("plotsWindowOpening", soon.size());
        facts.put("plotsSamplingHalfway", halfway.size());
        if (!open.isEmpty()) {
            var first = open.getFirst();
            facts.put("windowClosesOn", first.windowClosesOn().toString());
            if (single) {
                long percent = whole(first.percentageToRemove());
                facts.put("percentToRemove", percent);
                return text(questionId(), "thinning.open", TextDate.of(first.windowClosesOn()), percent);
            }
            return text(questionId(), "thinning.open.field", open.size(), active, TextDate.of(first.windowClosesOn()));
        }
        if (!soon.isEmpty()) {
            var first = soon.getFirst();
            facts.put("windowOpensOn", first.windowOpensOn().toString());
            if (single) {
                long percent = whole(first.percentageToRemove());
                facts.put("percentToRemove", percent);
                return text(questionId(), "thinning.opening", TextDate.of(first.windowOpensOn()), percent);
            }
            return text(questionId(), "thinning.opening.field", soon.size(), active,
                    TextDate.of(first.windowOpensOn()));
        }
        if (!halfway.isEmpty()) {
            if (single) {
                int missing = halfway.getFirst().treesNeeded();
                facts.put("treesNeeded", missing);
                return text(questionId(), "thinning.sampling", missing);
            }
            return text(questionId(), "thinning.sampling.field", halfway.size(), active);
        }
        return single ? text(questionId(), "thinning.none") : text(questionId(), "thinning.none.field", active);
    }

    /** The hottest forecast day of the plots (the earliest on a tie), with the forecast it comes from. */
    private static Optional<Hottest> hottest(HomeData home) {
        if (home.thresholds() == null) {
            return Optional.empty();
        }
        return home.plots().stream()
                .map(HomeData.PlotHome::forecast)
                .filter(forecast -> forecast != null)
                .flatMap(forecast -> forecast.days().stream().map(day -> new Hottest(forecast, day)))
                .min(Comparator.comparingDouble((Hottest hottest) -> -hottest.day().max())
                        .thenComparing(hottest -> hottest.day().date()));
    }

    private record Hottest(ClimateData.Forecast forecast, ClimateData.ForecastDay day) {
    }
}
