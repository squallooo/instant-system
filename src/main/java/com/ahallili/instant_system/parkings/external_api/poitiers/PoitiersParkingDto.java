package com.ahallili.instant_system.parkings.external_api.poitiers;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PoitiersParkingDto(
        @JsonProperty("infos_parkingsgeo_point") String parkingGeoPoint,
        @JsonProperty("_rand") Integer random,
        @JsonProperty("_geopoint") String geoPoint,
        @JsonProperty("Dernière_mise_à_jour_Base") Instant lastDatabaseUpdate,
        @JsonProperty("Capacite") Integer capacity,
        @JsonProperty("_i") Long index,
        @JsonProperty("Id") Integer id,
        @JsonProperty("Nom") String name,
        @JsonProperty("_updatedAt") Instant updatedAt,
        @JsonProperty("taux_doccupation") Double occupancyRate,
        @JsonProperty("Places") Integer availableSpots,
        @JsonProperty("_score") Double score,
        @JsonProperty("_id") String recordId) {
}
