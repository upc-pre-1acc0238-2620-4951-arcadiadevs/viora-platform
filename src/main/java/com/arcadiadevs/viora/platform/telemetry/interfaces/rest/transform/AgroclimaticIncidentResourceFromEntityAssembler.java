package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.application.queryservices.*;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.*;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Assembler mapping application query projections into presentation REST resources.
 */
public final class AgroclimaticIncidentResourceFromEntityAssembler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"));

    private AgroclimaticIncidentResourceFromEntityAssembler() {
    }

    /**
     * Converts a single {@link AgroclimaticIncidentItem} projection into an {@link AgroclimaticIncidentResource}.
     *
     * @param item the application query projection
     * @return presentation resource
     */
    public static AgroclimaticIncidentResource toResource(AgroclimaticIncidentItem item) {
        var snap = item.incident();
        var breach = snap.breachInfo();

        return new AgroclimaticIncidentResource(
                snap.id().incidentId(),
                snap.plotId().plotId(),
                item.plotName(),
                item.plotVariety(),
                snap.type().name(),
                snap.severity().name(),
                snap.status().name(),
                resolveHeadlineKey(snap.type()),
                breach.metricName(),
                breach.currentValue(),
                breach.thresholdValue(),
                breach.unit(),
                snap.triggeredAt(),
                DATE_FORMATTER.format(snap.triggeredAt()),
                resolveTimeWindow(snap.type()),
                snap.stressDurationMinutes(),
                snap.snoozedUntil()
        );
    }

    /**
     * Converts an {@link AgroclimaticIncidentsSummary} into an {@link AgroclimaticIncidentsSummaryResource}.
     *
     * @param summary the application query summary projection
     * @return presentation summary resource
     */
    public static AgroclimaticIncidentsSummaryResource toSummaryResource(AgroclimaticIncidentsSummary summary) {
        var countsResource = new SummaryCountsResource(
                summary.activeCount(),
                summary.criticalCount(),
                summary.warningCount(),
                summary.normalizedCount()
        );

        var incidentResources = summary.incidents().stream()
                .map(AgroclimaticIncidentResourceFromEntityAssembler::toResource)
                .toList();

        return new AgroclimaticIncidentsSummaryResource(countsResource, incidentResources);
    }

    /**
     * Converts an {@link AgroclimaticIncidentDetail} projection into an {@link AgroclimaticIncidentDetailResource}.
     *
     * @param detail        the application query detail projection
     * @param messageSource optional Spring MessageSource for resolving i18n templates
     * @param locale        the requested target locale
     * @return presentation detail resource
     */
    public static AgroclimaticIncidentDetailResource toDetailResource(
            AgroclimaticIncidentDetail detail,
            @Nullable MessageSource messageSource,
            Locale locale
    ) {
        var snap = detail.incident();
        var breach = snap.breachInfo();

        var stepResources = snap.mitigationSteps().stream()
                .map(stepSnap -> toStepResource(stepSnap, snap.triggeredAt(), detail.plotName(), messageSource, locale))
                .toList();

        var trendResources = detail.weeklyTrend().stream()
                .map(AgroclimaticIncidentResourceFromEntityAssembler::toTrendPointResource)
                .toList();

        return new AgroclimaticIncidentDetailResource(
                snap.id().incidentId(),
                snap.plotId().plotId(),
                detail.plotName(),
                detail.plotVariety(),
                snap.type().name(),
                snap.severity().name(),
                snap.status().name(),
                resolveHeadlineKey(snap.type()),
                breach.metricName(),
                breach.currentValue(),
                breach.thresholdValue(),
                breach.unit(),
                snap.triggeredAt(),
                DATE_FORMATTER.format(snap.triggeredAt()),
                resolveTimeWindow(snap.type()),
                snap.stressDurationMinutes(),
                snap.snoozedUntil(),
                stepResources,
                trendResources
        );
    }

    /**
     * Overload for {@link #toDetailResource(AgroclimaticIncidentDetail, MessageSource, Locale)}
     * using default locale and null messageSource for testing fallback.
     */
    public static AgroclimaticIncidentDetailResource toDetailResource(AgroclimaticIncidentDetail detail) {
        return toDetailResource(detail, null, Locale.getDefault());
    }

    public static MitigationStepResource toStepResource(
            MitigationStepSnapshot snapshot,
            Instant triggeredAt,
            String plotName,
            @Nullable MessageSource messageSource,
            Locale locale
    ) {
        String instruction = resolveInstruction(snapshot.instructionKey(), triggeredAt, plotName, messageSource, locale);
        return new MitigationStepResource(
                snapshot.id().stepId(),
                snapshot.instructionKey(),
                instruction,
                snapshot.completed(),
                snapshot.completedAt()
        );
    }

    public static MitigationStepResource toStepResource(MitigationStepSnapshot snapshot) {
        return toStepResource(snapshot, Instant.now(), "", null, Locale.getDefault());
    }

    private static String resolveInstruction(
            String instructionKey,
            Instant triggeredAt,
            String plotName,
            @Nullable MessageSource messageSource,
            Locale locale
    ) {
        if (messageSource == null) {
            return instructionKey;
        }

        var zoneId = java.time.ZoneId.of("America/Lima");
        var eventDate = triggeredAt.atZone(zoneId).toLocalDate();
        var prevDate = eventDate.minusDays(1);

        var prevDayName = prevDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, locale).toLowerCase(locale);
        var eventDayName = eventDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, locale).toLowerCase(locale);

        var timeWindow = "9 a. m.";
        if (locale.getLanguage().equalsIgnoreCase("en")) {
            timeWindow = "9:00 AM";
            prevDayName = capitalize(prevDayName);
            eventDayName = capitalize(eventDayName);
        }

        Object[] args = switch (instructionKey) {
            case "heat_wave.step.pre_irrigation", "frost_warning.step.pre_irrigate_soil" ->
                    new Object[]{prevDayName};
            case "heat_wave.step.morning_irrigation" ->
                    new Object[]{eventDayName, timeWindow};
            case "hydric_stress.step.check_drippers" ->
                    new Object[]{plotName.isBlank() ? "el lote" : plotName};
            case "frost_warning.step.activate_frost_protection" ->
                    new Object[]{"03:00 - 07:00"};
            default -> new Object[]{};
        };

        try {
            return messageSource.getMessage(instructionKey, args, locale);
        } catch (org.springframework.context.NoSuchMessageException ex) {
            return instructionKey;
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static WeeklyTrendPointResource toTrendPointResource(WeeklyTrendPoint point) {
        return new WeeklyTrendPointResource(
                point.timestamp(),
                point.value(),
                point.threshold()
        );
    }

    private static String resolveHeadlineKey(IncidentType type) {
        return switch (type) {
            case HYDRIC_STRESS -> "hydric_stress.headline";
            case HEAT_WAVE -> "heat_wave.headline";
            case FROST_WARNING -> "frost_warning.headline";
        };
    }

    private static String resolveTimeWindow(IncidentType type) {
        return switch (type) {
            case HEAT_WAVE -> "11:00 - 16:00";
            case FROST_WARNING -> "03:00 - 07:00";
            case HYDRIC_STRESS -> "06:00 - 10:00";
        };
    }
}
