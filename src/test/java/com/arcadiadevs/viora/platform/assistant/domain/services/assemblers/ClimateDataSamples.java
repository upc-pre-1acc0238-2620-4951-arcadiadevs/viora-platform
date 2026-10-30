package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Reading;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Thresholds;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;

/** Climate data of the ALERT and CLIMATE tests: mockup-like figures, not validated by an agronomist. */
public final class ClimateDataSamples {

    public static final Thresholds THRESHOLDS = new Thresholds(32.0, 36.0, 1.5);

    private ClimateDataSamples() {
    }

    /** Heat warning: forecast maximum 34.2 °C against 32 °C, raised on 28 Oct, 1 of 3 steps done. */
    public static Incident heat() {
        return new Incident("HEAT_WAVE", "WARNING", 34.2, 32.0, "°C", LocalDate.of(2026, 10, 28), 3, 1);
    }

    public static Incident frost() {
        return new Incident("FROST_WARNING", "CRITICAL", -2.4, -2.0, "°C", LocalDate.of(2026, 10, 29), 3, 0);
    }

    public static Incident hydric() {
        return new Incident("HYDRIC_STRESS", "WARNING", 18.5, 20.0, "%", LocalDate.of(2026, 10, 27), 0, 0);
    }

    /** Seven days from today; the maxima given flag heat at 32 °C and the minima frost at 1.5 °C. */
    public static Forecast forecast(double... maxima) {
        var days = new ArrayList<ForecastDay>();
        for (int i = 0; i < maxima.length; i++) {
            double min = maxima[i] < 10 ? maxima[i] - 8 : 14.0;
            days.add(new ForecastDay(TODAY.plusDays(i), maxima[i], min, maxima[i] >= 32.0, min <= 1.5));
        }
        return new Forecast(TODAY, days);
    }

    /** Hot for three days, then under the threshold. */
    public static Forecast hotThenMild() {
        return forecast(34.2, 35.1, 33.0, 30.4, 29.8, 28.0, 27.5);
    }

    public static SensorNode virtualProbe() {
        return new SensorNode("SOIL_PROBE", 30, true, 16.0, 20.0, "WARNING");
    }

    /** A measured probe at 30 cm (critical 16 %, warning 20 %); the risk is Telemetry's reading of the moisture. */
    public static SensorNode realProbe(String moistureRisk) {
        return new SensorNode("SOIL_PROBE", 30, false, 16.0, 20.0, moistureRisk);
    }

    public static SensorNode realProbe() {
        return realProbe("WARNING");
    }

    public static SensorNode microclimate() {
        return new SensorNode("MICROCLIMATE", null, true, null, null, null);
    }

    public static Reading reading(double soilMoisture) {
        return new Reading(27.4, 38.0, soilMoisture);
    }

    public static ClimateData climate(Incident incident, Forecast forecast, List<SensorNode> nodes,
            Reading reading) {
        return new ClimateData(THRESHOLDS, incident, incident == null ? 0 : 2, 5, forecast, nodes, reading);
    }

    public static PlotAnswerData data(ClimateData climate) {
        return new PlotAnswerData(PLOT, TODAY, 2026, null, null, null, null, climate);
    }
}
