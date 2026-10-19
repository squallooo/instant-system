package com.ahallili.instant_system.parkings.external_api.poitiers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.ahallili.instant_system.parkings.dto.ParkingDto;

class PoitierApiClientTest {

    private static final String API_URL = "https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel";
    private static final String RESPONSE = """
            {"total": 1, "results": [
                {"Id": 1, "Nom": "Centre", "Capacite": 100, "Places": 20,
                 "_geopoint": "46.58,0.34", "extra_field": "ignored"}
            ]}
            """;
    private final ParkingDto parking = new ParkingDto("1", "Centre", null, 100, 20,
            "46.58,0.34", null, "Poitiers", "France", null, null, null);
    private MockRestServiceServer server;
    private PoitierApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        // Intercept HTTP calls locally so tests never contact the real API.
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PoitierApiClient(builder.build(), API_URL);
    }

    @AfterEach
    void verifyRequests() throws Exception {
        server.verify();
    }

    // API fields are mapped to the common parking model; extra fields are ignored.
    @Test
    void mapsParkingList() throws Exception {
        server.expect(requestTo(API_URL + "/lines"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        assertEquals(List.of(parking), client.getParkings());
    }

    // An empty API result produces an empty list.
    @Test
    void returnsEmptyParkingList() throws Exception {
        server.expect(requestTo(API_URL + "/lines"))
                .andRespond(withSuccess("{\"total\":0,\"results\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(client.getParkings().isEmpty());
    }

    // The detail request filters on the requested parking identifier.
    @Test
    void fetchesParkingDetails() throws Exception {
        server.expect(requestTo(API_URL + "/lines?qs=Id:1"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        assertEquals(parking, client.getParkingDetails("1"));
    }

    // An unknown identifier produces an explicit error.
    @Test
    void rejectsUnknownParking() throws Exception {
        server.expect(requestTo(API_URL + "/lines?qs=Id:99"))
                .andRespond(withSuccess("{\"total\":0,\"results\":[]}", MediaType.APPLICATION_JSON));

        var exception = assertThrows(com.ahallili.instant_system.parkings.exception.ParkingNotFoundException.class,
                () -> client.getParkingDetails("99"));
        assertEquals("Parking not found for id: 99", exception.getMessage());
    }

    // The API expects longitude before latitude, followed by the radius in meters.
    @Test
    void fetchesNearbyParkings() throws Exception {
        server.expect(requestTo(API_URL + "/lines?geo_distance=0.34:46.58:500"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        assertEquals(List.of(parking), client.getNearByParkings("46.58,0.34", 500));
    }

    // Upstream server failures are propagated to the caller.
    @Test
    void propagatesApiFailure() throws Exception {
        server.expect(requestTo(API_URL + "/lines")).andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> client.getParkings());
    }
}
