package com.ahallili.instant_system.parkings.external_api.nantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class NantesApiClientTest {

    private static final String baseUrl = "https://data.nantesmetropole.fr/api/explore/v2.1/catalog/datasets/244400404_parkings-publics-nantes";
    private static final String URL = "https://data.nantesmetropole.fr/api/explore/v2.1/catalog/datasets/244400404_parkings-publics-nantes/records?limit=100&offset=";
    private static final String PARKING = """
            {"idobj":"134","nom_complet":"Parking Médiathèque","adresse":"Rue Neuve des Capucins",
             "commune":"Nantes","capacite_voiture":433,"lat_wgs84":47.21,"long_wgs84":-1.56,
             "telephone":"ignored"}
            """;
    private MockRestServiceServer server;
    private NantesApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NantesApiClient(builder.build(), baseUrl);
    }

    @AfterEach
    void verifyRequests() throws Exception {
        server.verify();
    }

    private void page(int offset, int total, String records) {
        server.expect(requestTo(URL + offset + "&order_by=idobj"))
                .andRespond(withSuccess("{\"total_count\":" + total + ",\"results\":[" + records + "]}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void mapsParkingWithoutInventingAvailability() throws Exception {
        page(0, 1, PARKING);
        var parking = client.getParkings().getFirst();
        assertEquals("134", parking.id());
        assertEquals("Parking Médiathèque", parking.name());
        assertEquals("Rue Neuve des Capucins", parking.address());
        assertEquals(433, parking.capacity());
        assertEquals("47.21,-1.56", parking.geoPoint());
        assertNull(parking.availableSpots());
        assertNull(parking.status());
        assertNull(parking.pricePerHour());
    }

    @Test
    void readsFollowingPagesAndHandlesMissingOptionalFields() throws Exception {
        page(0, 2, PARKING);
        page(1, 2, "{\"idobj\":\"3551\",\"nom_complet\":\"Baco\"}");
        var parkings = client.getParkings();
        assertEquals(2, parkings.size());
        assertNull(parkings.get(1).geoPoint());
        assertNull(parkings.get(1).capacity());
    }

    @Test
    void findsParkingById() throws Exception {
        page(0, 1, PARKING);
        assertEquals("134", client.getParkingDetails("134").id());
    }

    @Test
    void rejectsUnknownId() throws Exception {
        page(0, 1, PARKING);
        assertThrows(com.ahallili.instant_system.parkings.exception.ParkingNotFoundException.class,
                () -> client.getParkingDetails("missing"));
    }

    @Test
    void handlesEmptyDataset() throws Exception {
        page(0, 0, "");
        assertTrue(client.getParkings().isEmpty());
    }

    @Test
    void propagatesUpstreamFailure() throws Exception {
        server.expect(requestTo(URL + "0&order_by=idobj")).andRespond(withServerError());
        assertThrows(HttpServerErrorException.class, client::getParkings);
    }
}
