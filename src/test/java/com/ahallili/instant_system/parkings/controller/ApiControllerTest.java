package com.ahallili.instant_system.parkings.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.HttpStatus;

import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.dto.ParkingStatus;
import com.ahallili.instant_system.parkings.service.IUserService;
import com.ahallili.instant_system.parkings.service.JwtService;
import com.ahallili.instant_system.parkings.service.ParkingService;
import com.ahallili.instant_system.parkings.service.UserService;

class ApiControllerTest {

    private final ParkingService parkingService = mock(ParkingService.class);
    private final IUserService userService = mock(UserService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final ApiController controller = new ApiController(parkingService, userService, jwtService);
    private final ParkingDto parking = new ParkingDto("1", "Centre", "Poitiers", 100, 20,
            "46.58,0.34", ParkingStatus.OPEN, "Poitiers", "France", "", "", 0.0);

    @Test
    void logoutRevokesTheBearerToken() throws Exception {
        assertEquals(HttpStatus.OK, controller.logout("Bearer token").getStatusCode());
        verify(jwtService).revokeToken("token");
    }

    @Test
    void logoutWithoutTokenIsRejected() throws Exception {
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class, () -> controller.logout(null));
        verifyNoInteractions(jwtService);
    }

    // The health endpoint returns a successful response without calling the
    // service.
    @Test
    void returnsHealthMessage() throws Exception {
        var response = controller.hello();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("service is running", response.getBody());
        verifyNoInteractions(parkingService);
    }

    // Available cities are returned in the response body.
    @Test
    void returnsAvailableCities() throws Exception {
        when(parkingService.getSupportedCities()).thenReturn(List.of("POITIERS"));

        var response = controller.getAvailableCities();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("POITIERS"), response.getBody());
    }

    // Missing city values are rejected before the service is called.
    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingCity(String city) throws Exception {
        var response = controller.getCityParkings(city);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(parkingService);
    }

    // Unsupported cities return 404 without fetching parkings.
    @Test
    void rejectsUnsupportedCity() throws Exception {
        when(parkingService.getSupportedCities()).thenReturn(List.of("POITIERS"));

        var response = controller.getCityParkings("Paris");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(parkingService, never()).getParkings(anyString());
    }

    // City validation ignores case and returns the service result.
    @Test
    void returnsParkingsForSupportedCity() throws Exception {
        when(parkingService.getSupportedCities()).thenReturn(List.of("POITIERS"));
        when(parkingService.getParkings("poitiers")).thenReturn(List.of(parking));

        var response = controller.getCityParkings("poitiers");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(parking), response.getBody());
    }

    // City and parking identifier must be forwarded in the correct order.
    @Test
    void returnsParkingDetails() throws Exception {
        when(parkingService.getParkingDetails("Poitiers", "1")).thenReturn(parking);

        var response = controller.getParkingDetails("1", "Poitiers");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(parking, response.getBody());
    }

    // Nearby searches preserve the point and radius supplied by the caller.
    @Test
    void returnsNearbyParkings() throws Exception {
        when(parkingService.getNearByParkings("Poitiers", "46.58,0.34", 500)).thenReturn(List.of(parking));

        var response = controller.getNearByParkings("Poitiers", "46.58,0.34", 500);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(parking), response.getBody());
    }
}
