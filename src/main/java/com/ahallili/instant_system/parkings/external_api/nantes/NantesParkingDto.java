package com.ahallili.instant_system.parkings.external_api.nantes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NantesParkingDto(
        @JsonProperty("idobj") String id,
        @JsonProperty("nom_complet") String name,
        @JsonProperty("adresse") String address,
        @JsonProperty("commune") String city,
        @JsonProperty("capacite_voiture") Integer capacity,
        @JsonProperty("lat_wgs84") Double latitude,
        @JsonProperty("long_wgs84") Double longitude) {
}
