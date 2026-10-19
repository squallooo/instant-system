package com.ahallili.instant_system.parkings.dto;

public record AuthenticationResponseDto(String token, long expiresInMs, String role) {

}
