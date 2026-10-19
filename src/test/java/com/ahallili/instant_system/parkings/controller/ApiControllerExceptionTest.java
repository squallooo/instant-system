package com.ahallili.instant_system.parkings.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ahallili.instant_system.parkings.exception.ControllerExceptionHandler;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.service.IUserService;
import com.ahallili.instant_system.parkings.service.JwtService;
import com.ahallili.instant_system.parkings.service.ParkingService;

class ApiControllerExceptionTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "bad;500", "91,0;500", "0,181;500", "NaN,0;500", "46.58,0.34;0", "46.58,0.34;-1"
    }, delimiter = ';')
    void invalidLocationReturns400BeforeAnyExternalCall(String point, int radius) throws Exception {
        var factory = mock(com.ahallili.instant_system.parkings.external_api.ParkingClientFactory.class);
        var controller = new ApiController(new ParkingService(factory), userService, mock(JwtService.class));
        var localMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler()).build();

        localMvc.perform(get("/api/Poitiers/find_nearby_parkings")
                .param("geoPoint", point).param("radiusMeters", Integer.toString(radius)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(factory);
    }

    @Test
    void accessDeniedHasSame403BodyInControllerAndSecurityFilter() throws Exception {
        var exception = new org.springframework.security.access.AccessDeniedException("Internal permissions");
        when(parkingService.getSupportedCities()).thenThrow(exception);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        new com.ahallili.instant_system.parkings.conf.SecurityErrorHandler().handle(
                new org.springframework.mock.web.MockHttpServletRequest(), response, exception);

        org.junit.jupiter.api.Assertions.assertEquals(403, response.getStatus());
        mvc.perform(get("/api/available-cities"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(response.getContentAsString()));
    }

    private final ParkingService parkingService = mock(ParkingService.class);
    private final IUserService userService = mock(IUserService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new ApiController(parkingService, userService, mock(JwtService.class)))
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @Test
    void returns404WhenParkingDoesNotExist() throws Exception {
        when(parkingService.getParkingDetails("Nantes", "missing"))
                .thenThrow(new ParkingNotFoundException("Parking missing"));

        mvc.perform(get("/api/Nantes/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No result found"));
    }

    @Test
    void returns401ForIncorrectCredentials() throws Exception {
        when(userService.login("test@example.com", "wrong"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"test@example.com","pwd":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns400WhenRadiusIsMissing() throws Exception {
        mvc.perform(get("/api/Nantes/find_nearby_parkings").param("geoPoint", "47.21,-1.56"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(parkingService);
    }

    @Test
    void returns500WithoutExposingInternalError() throws Exception {
        when(parkingService.getSupportedCities()).thenThrow(new RuntimeException("Internal database details"));

        mvc.perform(get("/api/available-cities"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal error. Please contact the admin if the issue remains"));
    }
}
