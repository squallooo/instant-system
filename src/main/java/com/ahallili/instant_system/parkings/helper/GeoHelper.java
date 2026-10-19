package com.ahallili.instant_system.parkings.helper;

import java.util.Locale;

import com.ahallili.instant_system.parkings.exception.InvalidLocationException;

public class GeoHelper {

    // Approximate distance in kilometers for one degree of latitude.
    private static final double KM_PER_DEGREE = 111.32;

    /**
     * Remove surrounding whitespace and quotes from external API coordinates.
     * 
     * @param geoPoint
     * @return
     */
    public static String cleanGeoPoint(String geoPoint) {

        // Keep the comma separator so latitude and longitude can be parsed later.
        String clean = geoPoint.trim().replaceAll("\"", "");
        return clean;
    }

    public static double getLatitude(String geoPoint) {

        return getAtIndex(geoPoint, 0);
    }

    public static double getLongitude(String geoPoint) {
        return getAtIndex(geoPoint, 1);
    }

    public static double getAtIndex(String geoPoint, int index) {
        return parsePoint(geoPoint)[index];
    }

    public static double[] parsePoint(String geoPoint) {
        if (geoPoint == null || geoPoint.isBlank()) {
            throw new InvalidLocationException("geoPoint is required (latitude,longitude)");
        }
        String[] parts = cleanGeoPoint(geoPoint).split(",", -1);
        if (parts.length != 2) {
            throw new InvalidLocationException("geoPoint must contain latitude,longitude");
        }
        try {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                throw new InvalidLocationException("Latitude must be between -90 and 90, longitude between -180 and 180");
            }
            return new double[] { latitude, longitude };
        } catch (NumberFormatException exception) {
            throw new InvalidLocationException("Coordinates must be numbers");
        }
    }

    // Haversine distance on Earth, in meters (not road distance).
    public static double distanceInMeters(double[] origin, double[] destination) {
        double lat1 = Math.toRadians(origin[0]);
        double lat2 = Math.toRadians(destination[0]);
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(destination[1] - origin[1]);
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        // Clamp rounding errors near antipodal points.
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }

    public static String calculateBoundingBox(
            double latitude,
            double longitude,
            double radiusKm) {

        double latDelta = radiusKm / KM_PER_DEGREE;

        // Longitude degrees cover a shorter distance as latitude increases.
        double lonDelta = radiusKm /
                (KM_PER_DEGREE * Math.cos(Math.toRadians(latitude)));

        double latMin = latitude - latDelta;
        double latMax = latitude + latDelta;

        double lonMin = longitude - lonDelta;
        double lonMax = longitude + lonDelta;

        return String.format(
                Locale.US,
                "%.6f,%.6f,%.6f,%.6f",
                lonMin, latMin, lonMax, latMax);
    }

}
