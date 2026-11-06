package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * A point on the map, such as where the manager stands or the center of a plot.
 *
 * @param latitude  degrees, from -90 to 90
 * @param longitude degrees, from -180 to 180
 */
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Validates the ranges.
     *
     * @throws IllegalArgumentException if a coordinate is out of range or not a number
     */
    public GeoPoint {
        if (Double.isNaN(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("territory.location.latitude.invalid");
        }
        if (Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("territory.location.longitude.invalid");
        }
    }

    /**
     * Distance along the Earth's surface to another point.
     *
     * @param other the other point
     * @return the distance in kilometers
     */
    public double distanceKmTo(GeoPoint other) {
        var dLat = Math.toRadians(other.latitude - latitude);
        var dLon = Math.toRadians(other.longitude - longitude);
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
