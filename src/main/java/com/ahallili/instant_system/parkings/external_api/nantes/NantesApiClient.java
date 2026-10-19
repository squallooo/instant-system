package com.ahallili.instant_system.parkings.external_api.nantes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.external_api.IParkingApiClient;

@Component
public class NantesApiClient implements IParkingApiClient {

    private static final int PAGE_SIZE = 100;
    private final RestClient restClient;

    public NantesApiClient(RestClient restClient, @Value("${client.api.nantes.url}") String url) {
        this.restClient = restClient.mutate().baseUrl(url).build();
    }

    @Override
    public String getCityNameImpl() {
        return "Nantes";
    }

    @Override
    public ParkingDto getParkingDetails(String parkingId) throws ParkingNotFoundException {
        return getParkings().stream()
                .filter(parking -> Objects.equals(parking.id(), parkingId))
                .findFirst()
                .orElseThrow(() -> new ParkingNotFoundException("Parking not found for id: " + parkingId));
    }

    @Override
    public List<ParkingDto> getParkings() throws ParkingNotFoundException {
        List<ParkingDto> parkings = new ArrayList<>();
        int offset = 0;
        NantesResponseDto response;
        do {
            response = restClient.get()
                    .uri("/records?limit={limit}&offset={offset}&order_by=idobj", PAGE_SIZE, offset)
                    .retrieve()
                    .body(NantesResponseDto.class);
            if (response == null || response.results() == null) {
                throw new ParkingNotFoundException("Empty response from Nantes parking API");
            }
            parkings.addAll(response.results().stream().map(this::toParkingDto).toList());
            offset += response.results().size();
        } while (!response.results().isEmpty() && offset < response.totalCount());
        return parkings;
    }

    private ParkingDto toParkingDto(NantesParkingDto parking) {
        String geoPoint = parking.latitude() == null || parking.longitude() == null
                ? null
                : parking.latitude() + "," + parking.longitude();
        // This dataset does not provide live availability, status or structured
        // prices/hours.
        return new ParkingDto(parking.id(), parking.name(), parking.address(), parking.capacity(),
                null, geoPoint, null, parking.city(), "France", null, null, null);
    }

    @Override
    public boolean canHandleNearByParkings() {
        return false;
    }

    @Override
    public List<ParkingDto> getNearByParkings(String geoPoint, int radiusMeters)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Nearby parkings not supported for Nantes");
    }

}
