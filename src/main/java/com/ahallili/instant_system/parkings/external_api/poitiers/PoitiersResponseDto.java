package com.ahallili.instant_system.parkings.external_api.poitiers;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PoitiersResponseDto(Long total, List<PoitiersParkingDto> results) {
}
