package com.arcadiadevs.viora.platform.orchard.domain.services;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Domain service providing geometric verification and geodesic surface area calculation for cadastral geometries.
 */
public class CadastralGeometryService {

    private static final Pattern COORD_PAIR_PATTERN = Pattern.compile("\\[\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\]");

    /**
     * Validates that the provided GeoJSON polygon has a valid closed ring topology with minimum required vertices.
     *
     * @param geoJson the raw GeoJSON polygon string
     * @throws InvalidPlotGeometryException if the geometry is empty, has insufficient vertices, or is unclosed
     */
    public void validate(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            throw new InvalidPlotGeometryException("plot.geometry.empty");
        }

        var coords = parseCoordinates(geoJson);
        if (coords.size() < 4) {
            throw new InvalidPlotGeometryException("plot.geometry.vertices_min");
        }

        double[] first = coords.get(0);
        double[] last = coords.get(coords.size() - 1);
        if (Math.abs(first[0] - last[0]) > 1e-6 || Math.abs(first[1] - last[1]) > 1e-6) {
            throw new InvalidPlotGeometryException("plot.geometry.ring_unclosed");
        }
    }

    /**
     * Calculates geodesic net surface area in hectares for a valid GeoJSON polygon.
     *
     * @param geoJson the raw GeoJSON polygon string
     * @return the calculated surface area in hectares (minimum 0.01 ha)
     */
    public Double netAreaHa(String geoJson) {
        validate(geoJson);
        var coords = parseCoordinates(geoJson);
        double areaSqMeters = calculateGeodesicArea(coords);
        double areaHa = Math.round((areaSqMeters / 10000.0) * 100.0) / 100.0;
        return areaHa <= 0.0 ? 0.01 : areaHa;
    }

    /**
     * Convenience method that validates the GeoJSON geometry and creates an encapsulated {@link PlotGeometry} value object.
     *
     * @param geoJson the raw GeoJSON polygon string
     * @return a valid {@link PlotGeometry} value object
     */
    public PlotGeometry computeGeometry(String geoJson) {
        double areaHa = netAreaHa(geoJson);
        return new PlotGeometry(geoJson, areaHa);
    }

    /**
     * Computes the centroid coordinates [latitude, longitude] for a valid GeoJSON polygon.
     *
     * @param geoJson the raw GeoJSON polygon string
     * @return double array where index 0 is latitude and index 1 is longitude
     */
    public double[] computeCentroid(String geoJson) {
        validate(geoJson);
        var coords = parseCoordinates(geoJson);
        double sumLat = 0.0;
        double sumLon = 0.0;
        int n = coords.size() - 1;
        if (n < 3) {
            n = coords.size();
        }
        for (int i = 0; i < n; i++) {
            double[] pt = coords.get(i);
            sumLon += pt[0];
            sumLat += pt[1];
        }
        return new double[]{sumLat / n, sumLon / n};
    }

    private List<double[]> parseCoordinates(String geoJson) {
        var matcher = COORD_PAIR_PATTERN.matcher(geoJson);
        List<double[]> coords = new ArrayList<>();
        while (matcher.find()) {
            double lon = Double.parseDouble(matcher.group(1));
            double lat = Double.parseDouble(matcher.group(2));
            coords.add(new double[]{lon, lat});
        }
        return coords;
    }

    private double calculateGeodesicArea(List<double[]> coords) {
        double total = 0.0;
        int n = coords.size();
        double avgLat = 0.0;
        for (double[] pt : coords) {
            avgLat += pt[1];
        }
        avgLat /= n;
        double metersPerLatDegree = 111320.0;
        double metersPerLonDegree = 111320.0 * Math.cos(Math.toRadians(avgLat));

        for (int i = 0; i < n - 1; i++) {
            double[] p1 = coords.get(i);
            double[] p2 = coords.get(i + 1);
            double x1 = p1[0] * metersPerLonDegree;
            double y1 = p1[1] * metersPerLatDegree;
            double x2 = p2[0] * metersPerLonDegree;
            double y2 = p2[1] * metersPerLatDegree;
            total += (x1 * y2 - x2 * y1);
        }
        return Math.abs(total) / 2.0;
    }
}
