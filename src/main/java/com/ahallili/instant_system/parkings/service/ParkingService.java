package com.ahallili.instant_system.parkings.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.exception.InvalidLocationException;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.external_api.ParkingClientFactory;
import com.ahallili.instant_system.parkings.helper.GeoHelper;

/**
 * Service class responsible for handling parking-related operations.
 * It interacts with the ParkingClientFactory to fetch parking details and lists
 * ParkingService
 */
@Service
public class ParkingService {

    private final ParkingClientFactory clientFactory;

    public ParkingService(ParkingClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ParkingDto getParkingDetails(String city, String parkingId) throws ParkingNotFoundException {
        return clientFactory.getClient(city).getParkingDetails(parkingId);
    }

    /**
     * 
     * caching this method as it pulls all the parkings for optimization matter
     * 
     * @param city
     * @return
     * @throws ParkingNotFoundException
     */
    @Cacheable(cacheNames = "parkings", key = "#city.toUpperCase(T(java.util.Locale).ROOT)")
    public List<ParkingDto> getParkings(String city) throws ParkingNotFoundException {
        return clientFactory.getClient(city).getParkings();
    }

    public List<String> getSupportedCities() {
        return clientFactory.getSupportedCities().stream().toList();
    }

    public boolean canHandleNearByParkings(String city) {
        return clientFactory.getClient(city).canHandleNearByParkings();
    }

    public List<ParkingDto> getNearByParkings(String city, String geoPoint, int radiusMeters)
            throws ParkingNotFoundException {
        double[] origin = GeoHelper.parsePoint(geoPoint);
        if (radiusMeters <= 0) {
            throw new InvalidLocationException("radiusMeters must be greater than zero (meters)");
        }
        if (canHandleNearByParkings(city)) {
            return clientFactory.getClient(city).getNearByParkings(geoPoint, radiusMeters);
        }
        List<NearbyParking> nearby = new ArrayList<>();
        for (ParkingDto parking : clientFactory.getClient(city).getParkings()) {
            try {
                double distance = GeoHelper.distanceInMeters(origin, GeoHelper.parsePoint(parking.geoPoint()));
                if (distance <= radiusMeters) {
                    nearby.add(new NearbyParking(parking, distance));
                }
            } catch (InvalidLocationException exception) {
                // Skip source records that cannot be located; the request was validated above.
            }
        }
        return nearby.stream().sorted(Comparator.comparingDouble(NearbyParking::distance))
                .map(NearbyParking::parking).toList();
    }

    private record NearbyParking(ParkingDto parking, double distance) {
    }

}
