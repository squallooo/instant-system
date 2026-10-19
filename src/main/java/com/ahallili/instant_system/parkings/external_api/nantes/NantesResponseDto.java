package com.ahallili.instant_system.parkings.external_api.nantes;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NantesResponseDto(@JsonProperty("total_count") int totalCount, List<NantesParkingDto> results) {
}
