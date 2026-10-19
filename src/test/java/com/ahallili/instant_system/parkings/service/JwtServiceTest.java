package com.ahallili.instant_system.parkings.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import com.ahallili.instant_system.parkings.conf.SecurityErrorHandler;

import com.ahallili.instant_system.parkings.conf.JwtAuthenticationFilter;

import jakarta.servlet.FilterChain;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;

class JwtServiceTest {
    @Test
    void rejectsTokenForAnotherUser() {
        JwtService service = service();
        var owner = User.withUsername("owner@example.com").password("unused").authorities("USER").build();
        var other = User.withUsername("other@example.com").password("unused").authorities("USER").build();

        assertFalse(service.isTokenValid(service.generateToken(owner), other));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = service();
        ReflectionTestUtils.setField(service, "jwtExpiration", -60000L);
        var user = User.withUsername("test@example.com").password("unused").authorities("USER").build();
        String token = service.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> service.isTokenValid(token, user));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService service = service();
        var user = User.withUsername("test@example.com").password("unused").authorities("USER").build();
        String token = service.generateToken(user);
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        ReflectionTestUtils.setField(service, "secretKey", Base64.getEncoder().encodeToString(otherKey));

        assertThrows(SignatureException.class, () -> service.isTokenValid(token, user));
    }

    private JwtService service() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", Base64.getEncoder().encodeToString(new byte[32]));
        ReflectionTestUtils.setField(service, "jwtExpiration", 60000L);
        return service;
    }

    @Test
    void revocationInvalidatesOnlyTheLoggedOutToken() {
        JwtService service = service();
        var user = User.withUsername("test@example.com").password("unused").authorities("USER").build();
        String token = service.generateToken(user);
        String otherToken = service.generateToken(user);
        assertTrue(service.isTokenValid(token, user));

        service.revokeToken(token);

        assertFalse(service.isTokenValid(token, user));
        assertTrue(service.isTokenValid(otherToken, user));
        assertTrue(service.isTokenValid(service.generateToken(user), user));
    }

    @Test
    void revokedTokenIsRejectedBeforeReachingEndpoint() throws Exception {
        JwtService service = service();
        var user = User.withUsername("test@example.com").password("unused").authorities("USER").build();
        String token = service.generateToken(user);
        service.revokeToken(token);
        var users = mock(UserDetailsService.class);
        var filter = new JwtAuthenticationFilter(service, users, new SecurityErrorHandler());
        var request = new MockHttpServletRequest("GET", "/api/user/users");
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain, users);
    }
}
