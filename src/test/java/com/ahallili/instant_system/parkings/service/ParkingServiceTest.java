package com.ahallili.instant_system.parkings.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.dto.ParkingStatus;
import com.ahallili.instant_system.parkings.external_api.IParkingApiClient;
import com.ahallili.instant_system.parkings.external_api.ParkingClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ahallili.instant_system.parkings.exception.InvalidLocationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;

class ParkingServiceTest {

    private final IParkingApiClient client = mock(IParkingApiClient.class);
    private final ParkingDto parking = new ParkingDto("1", "Centre", "Poitiers", 100, 20,
            "46.58,0.34", ParkingStatus.OPEN, "Poitiers", "France", "", "", 0.0);
    private ParkingService service;

    @BeforeEach
    void setUp() throws Exception {
        when(client.getCityNameImpl()).thenReturn("Poitiers");
        service = new ParkingService(new ParkingClientFactory(List.of(client)));
    }

    // The service returns the parking list supplied by the selected city client.
    @Test
    void returnsCityParkings() throws Exception {
        when(client.getParkings()).thenReturn(List.of(parking));

        assertEquals(List.of(parking), service.getParkings("Poitiers"));
    }

    // The requested parking identifier is forwarded to the client.
    @Test
    void returnsParkingDetails() throws Exception {
        when(client.getParkingDetails("1")).thenReturn(parking);

        assertEquals(parking, service.getParkingDetails("Poitiers", "1"));
    }

    // Supported cities come from the registered clients.
    @Test
    void returnsSupportedCities() throws Exception {
        assertEquals(List.of("POITIERS"), service.getSupportedCities());
    }

    // Clients supporting nearby searches receive the original point and radius.
    @Test
    void delegatesNearbySearchWhenSupported() throws Exception {
        when(client.canHandleNearByParkings()).thenReturn(true);
        when(client.getNearByParkings("46.58,0.34", 500)).thenReturn(List.of(parking));

        assertEquals(List.of(parking), service.getNearByParkings("Poitiers", "46.58,0.34", 500));
        verify(client, never()).getParkings();
    }

    // The fallback filters and sorts locally, ignoring unusable source coordinates.
    @Test
    void filtersNearbyParkingsWhenSearchIsUnsupported() throws Exception {
        when(client.canHandleNearByParkings()).thenReturn(false);
        ParkingDto nearby = at("nearby", "46.581,0.34");
        when(client.getParkings()).thenReturn(List.of(at("far", "47,0.34"), nearby,
                at("unknown", null), at("invalid", "bad"), parking));

        assertEquals(List.of(parking, nearby), service.getNearByParkings("Poitiers", "46.58,0.34", 500));
        verify(client, never()).getNearByParkings(anyString(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void rejectsNonPositiveRadiusBeforeCallingProvider(int radius) throws Exception {
        assertThrows(InvalidLocationException.class,
                () -> service.getNearByParkings("Poitiers", "46.58,0.34", radius));
        verify(client, never()).getParkings();
        verify(client, never()).getNearByParkings(anyString(), anyInt());
    }

    @Test
    void rejectsInvalidPointBeforeCallingProvider() throws Exception {
        assertThrows(InvalidLocationException.class,
                () -> service.getNearByParkings("Poitiers", "91,0", 500));
        verify(client, never()).getParkings();
        verify(client, never()).getNearByParkings(anyString(), anyInt());
    }

    private ParkingDto at(String id, String point) {
        return new ParkingDto(id, id, null, null, null, point, null, "Poitiers", "France", null, null, null);
    }
}
