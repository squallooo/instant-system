package com.ahallili.instant_system.parkings.external_api;

import java.util.List;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.helper.GeoHelper;

/**
 * IParkingApiClient is an interface that defines methods for fetching parking
 * information from external APIs.
 * Implementations of this interface should provide city-specific logic for
 * retrieving parking details and lists.
 * IParkingApiClient
 */
public interface IParkingApiClient {

    public String getCityNameImpl();

    public ParkingDto getParkingDetails(String parkingId) throws ParkingNotFoundException;

    public List<ParkingDto> getParkings() throws ParkingNotFoundException;

    /**
     * Indicates whether the implementation can handle fetching nearby parkings.
     * Implementations that support nearby parkings should return true, while those
     * that do not should return false.
     * this allows the service to know if it can call getNearByParkings or not
     * in order to use a fallback to avoid throwing an exception
     * 
     * @return
     */
    public boolean canHandleNearByParkings();

    /**
     * Fetches a list of nearby parkings based on the provided latitude and
     * longitude.
     * Implementations should provide the logic to retrieve nearby parking
     * information
     * If the implementation does not support nearby parkings, it should throw an
     * UnsupportedOperationException.
     * 
     * @param latitude
     * @param longitude
     * @param radiusMeters    in meter
     * @return
     */
    public List<ParkingDto> getNearByParkings(String geoPoint, int radiusMeters)
            throws UnsupportedOperationException;

    /**
     * Helper to have a standard way to calculate the bounding box
     * 
     * @param latitude
     * @param longitude
     * @param radiusMeters    in meter
     * @return
     */
    default public String getBoundingBox(String geoPoint, int radiusMeters) {
        return GeoHelper.calculateBoundingBox(GeoHelper.getLatitude(geoPoint), GeoHelper.getLongitude(geoPoint),
                radiusMeters);
    }
}
