package com.ahallili.instant_system.parkings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParkingDto(String id, String name, String address, Integer capacity, Integer availableSpots,
                String geoPoint, ParkingStatus status, String city, String country, String openingHours,
                String closingHours, Double pricePerHour) {

}
