package com.ahallili.instant_system.parkings.dto;

import java.util.List;

public record CityDto(String name, String country, double latitude, double longitude, List<String> parkingIds) {

}
