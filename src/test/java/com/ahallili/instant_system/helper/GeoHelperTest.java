package com.ahallili.instant_system.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ahallili.instant_system.parkings.exception.InvalidLocationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.ahallili.instant_system.parkings.helper.GeoHelper;

class GeoHelperTest {

    // Coordinates may contain spaces and quotes in external API responses.
    @Test
    void readsCoordinatesWithSpacesAndQuotes() {
        String point = "  \"46.58, 0.34\"  ";

        assertEquals("46.58, 0.34", GeoHelper.cleanGeoPoint(point));
        assertEquals(46.58, GeoHelper.getLatitude(point));
        assertEquals(0.34, GeoHelper.getLongitude(point));
    }

    // Invalid input must never silently become a point at latitude/longitude zero.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "invalid", "46.58", "1,2,3", "91,0", "-91,0", "0,181", "0,-181",
            "NaN,0", "0,Infinity", "1,", ",2" })
    void rejectsInvalidCoordinates(String point) {
        assertThrows(InvalidLocationException.class, () -> GeoHelper.parsePoint(point));
    }

    @Test
    void calculatesDistanceInMeters() {
        assertEquals(0, GeoHelper.distanceInMeters(new double[] { 0, 0 }, new double[] { 0, 0 }), 0.001);
        assertEquals(111195, GeoHelper.distanceInMeters(new double[] { 0, 0 }, new double[] { 0, 1 }), 1);
        assertEquals(222390, GeoHelper.distanceInMeters(new double[] { 0, 179 }, new double[] { 0, -179 }), 1);
    }

    // At the equator, 111.32 km corresponds to one degree on both axes.
    @Test
    void calculatesBoundingBoxAtEquator() {
        assertEquals("-1.000000,-1.000000,1.000000,1.000000",
                GeoHelper.calculateBoundingBox(0, 0, 111.32));
    }

    // Longitude distances shrink with latitude; output uses longitude first.
    @Test
    void calculatesBoundingBoxAwayFromEquator() {
        assertEquals("8.000000,59.000000,12.000000,61.000000",
                GeoHelper.calculateBoundingBox(60, 10, 111.32));
    }

    // A zero radius produces a box containing only the original point.
    @Test
    void calculatesBoundingBoxWithZeroRadius() {
        assertEquals("0.340000,46.580000,0.340000,46.580000",
                GeoHelper.calculateBoundingBox(46.58, 0.34, 0));
    }
}
