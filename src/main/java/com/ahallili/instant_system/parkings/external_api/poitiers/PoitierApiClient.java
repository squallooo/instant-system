package com.ahallili.instant_system.parkings.external_api.poitiers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.external_api.IParkingApiClient;
import com.ahallili.instant_system.parkings.helper.GeoHelper;

/**
 * Class PoitierApiClient implements the IParkingApiClient interface to provide
 * parking information for the city of Poitiers.
 * It uses a RestClient to fetch data from the Poitiers parking API.
 * PoitierApiClient
 */
@Component
public class PoitierApiClient implements IParkingApiClient {

        private final Logger logger = LoggerFactory.getLogger(PoitierApiClient.class);

        private RestClient restClient;

        public PoitierApiClient(RestClient restClient, @Value("${client.api.poitiers.url}") String url) {
                this.restClient = restClient.mutate().baseUrl(url).build();

        }

        @Override
        public String getCityNameImpl() {
                return "Poitiers";
        }

        @Override
        public ParkingDto getParkingDetails(String parkingId) throws ParkingNotFoundException {
                PoitiersResponseDto results = restClient.get()
                                .uri("/lines?qs=Id:" + parkingId)
                                .retrieve()
                                .body(PoitiersResponseDto.class);
                PoitiersParkingDto res = results.results().stream().findFirst()
                                .orElseThrow(() -> new ParkingNotFoundException(
                                                "Parking not found for id: " + parkingId));
                return toParkingDto(res);
        }

        @Override
        public List<ParkingDto> getParkings() {
                PoitiersResponseDto response = restClient.get()
                                .uri("/lines")
                                .retrieve()
                                .body(PoitiersResponseDto.class);
                return response.results().stream()
                                .map(this::toParkingDto)
                                .collect(Collectors.toList());
        }

        @Override
        public boolean canHandleNearByParkings() {
                return true;
        }

        @Override
        public List<ParkingDto> getNearByParkings(String geoPoint, int radiusMeters)
                        throws UnsupportedOperationException {

                String latitude = "" + GeoHelper.getLatitude(geoPoint);
                String longitude = "" + GeoHelper.getLongitude(geoPoint);

                PoitiersResponseDto response = restClient.get()
                                .uri("/lines?geo_distance=" + longitude + ":" + latitude + ":" + radiusMeters)
                                .retrieve()
                                .body(PoitiersResponseDto.class);

                logger.info(geoPoint);

                return response.results().stream()
                                .map(this::toParkingDto)
                                .collect(Collectors.toList());
        }

        private ParkingDto toParkingDto(PoitiersParkingDto parking) {
                // Keep unknown information null instead of inventing prices or opening status.
                return new ParkingDto(parking.id() == null ? null : parking.id().toString(), parking.name(), null,
                                parking.capacity(), parking.availableSpots(), parking.geoPoint(), null,
                                "Poitiers", "France", null, null, null);
        }
}
