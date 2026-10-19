package com.ahallili.instant_system.parkings.external_api;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkingClientFactoryTest {

    private final IParkingApiClient client = mock(IParkingApiClient.class);
    private ParkingClientFactory factory;

    @BeforeEach
    void setUp() {
        when(client.getCityNameImpl()).thenReturn("Poitiers");
        factory = new ParkingClientFactory(List.of(client));
    }

    // City lookup accepts any letter case.
    @ParameterizedTest
    @ValueSource(strings = {"Poitiers", "poitiers", "POITIERS"})
    void findsClientIgnoringCase(String city) {
        assertSame(client, factory.getClient(city));
    }

    // Unknown cities fail with a message identifying the requested city.
    @Test
    void rejectsUnsupportedCity() {
        var exception = assertThrows(IllegalArgumentException.class, () -> factory.getClient("Paris"));
        assertEquals("city not supported : Paris", exception.getMessage());
    }

    // Only registered cities are exposed, using uppercase names.
    @Test
    void returnsSupportedCities() {
        assertEquals(Set.of("POITIERS"), factory.getSupportedCities());
    }
}
