package com.ahallili.instant_system.parkings.external_api;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Factory class responsible for providing instances of IParkingApiClient based
 * on the city name.
 * It maintains a mapping of city names to their corresponding API client
 * implementations.
 * ParkingClientFactory
 */
@Component
public class ParkingClientFactory {

    private final Map<String, IParkingApiClient> parkingApiClients;

    /**
     * Constructor for ParkingClientFactory.
     * It initializes the mapping of city names to their corresponding API client
     * implementations.
     * 
     * @param implementations a list of IParkingApiClient implementations to be
     *                        registered in the factory. injected by spring boot
     *                        automatically.
     */
    public ParkingClientFactory(List<IParkingApiClient> implementations) {
        this.parkingApiClients = implementations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        client -> client.getCityNameImpl().toUpperCase(),
                        Function.identity()));
    }

    /**
     * this method retrieves the appropriate IParkingApiClient implementation based
     * on the provided city name.
     * 
     * @param city
     * @return
     */
    public IParkingApiClient getClient(String city) {
        IParkingApiClient client = parkingApiClients.get(
                city.toUpperCase(Locale.ROOT));

        if (client == null) {
            throw new IllegalArgumentException(
                    "city not supported : " + city);
        }

        return client;
    }

    /**
     * this method returns the set of supported cities by the factory.
     * it ensures that the city names are returned in uppercase format for
     * consistency.
     * it ensures that the set of supported cities is unmodifiable, preventing
     * external modifications.
     * it provides a way to retrieve the list of cities for which the factory has
     * registered API clients. and ensures that the city names returned have an
     * implementation in the factory.
     * 
     * @return
     */
    public Set<String> getSupportedCities() {
        return parkingApiClients.keySet();
    }

}
