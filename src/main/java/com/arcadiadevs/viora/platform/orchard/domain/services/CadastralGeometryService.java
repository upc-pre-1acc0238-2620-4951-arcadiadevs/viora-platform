package com.arcadiadevs.viora.platform.orchard.domain.services;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotGeometry;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain service providing geometric verification and geodesic surface area calculation for cadastral geometries.
 */
public class CadastralGeometryService {

    /** Longest GeoJSON text accepted; a hand-drawn plot outline is a few kilobytes at most. */
    public static final int MAX_GEOJSON_LENGTH = 50_000;

    /** Most vertices accepted in the outline ring, the closing one included. */
    public static final int MAX_VERTICES = 500;

    /** Largest plot accepted; far above any olive plot, so a bigger one is a drawing or typing error. */
    public static final double MAX_AREA_HA = 5_000.0;

    private static final JsonMapper JSON = JsonMapper.builder().build();

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
        if (coords.size() > MAX_VERTICES) {
            throw new InvalidPlotGeometryException("plot.geometry.vertices_max");
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
        if (areaHa > MAX_AREA_HA) {
            throw new InvalidPlotGeometryException("plot.geometry.area_too_large");
        }
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

    /**
     * Reads the outline ring of a GeoJSON Polygon: {@code {"type":"Polygon","coordinates":[[[lon,lat],...]]}}.
     * Only the outer ring is accepted, since the area ignores holes, and every position must be a real WGS84
     * longitude and latitude.
     */
    private List<double[]> parseCoordinates(String geoJson) {
        if (geoJson.length() > MAX_GEOJSON_LENGTH) {
            throw new InvalidPlotGeometryException("plot.geometry.too_large");
        }
        final JsonNode root;
        try {
            root = JSON.readTree(geoJson);
        } catch (JacksonException exception) {
            throw new InvalidPlotGeometryException("plot.geometry.invalid");
        }
        var type = root == null ? null : root.get("type");
        if (type == null || !type.isString() || !"Polygon".equals(type.stringValue())) {
            throw new InvalidPlotGeometryException("plot.geometry.invalid");
        }
        var rings = root.get("coordinates");
        if (rings == null || !rings.isArray() || rings.size() != 1 || !rings.get(0).isArray()) {
            throw new InvalidPlotGeometryException("plot.geometry.invalid");
        }
        var ring = rings.get(0);
        if (ring.size() > MAX_VERTICES) {
            throw new InvalidPlotGeometryException("plot.geometry.vertices_max");
        }
        List<double[]> coords = new ArrayList<>(ring.size());
        for (JsonNode position : ring) {
            if (!position.isArray() || position.size() < 2 || !position.get(0).isNumber()
                    || !position.get(1).isNumber()) {
                throw new InvalidPlotGeometryException("plot.geometry.invalid");
            }
            double lon = position.get(0).doubleValue();
            double lat = position.get(1).doubleValue();
            if (!Double.isFinite(lon) || !Double.isFinite(lat) || Math.abs(lon) > 180.0 || Math.abs(lat) > 90.0) {
                throw new InvalidPlotGeometryException("plot.geometry.coordinates_out_of_range");
            }
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
