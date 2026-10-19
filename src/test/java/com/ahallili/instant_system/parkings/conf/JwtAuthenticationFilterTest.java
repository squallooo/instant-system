package com.ahallili.instant_system.parkings.conf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.ahallili.instant_system.parkings.service.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users, new SecurityErrorHandler());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidTokenReturns401AndStopsRequest() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        when(jwtService.extractUsername("invalid")).thenThrow(new MalformedJwtException("details"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals(SecurityErrorHandler.UNAUTHORIZED_MESSAGE, response.getContentAsString());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        verifyNoInteractions(chain);
    }

    @Test
    void downstreamErrorIsNotTurnedIntoAuthenticationError() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        var response = new MockHttpServletResponse();
        var user = User.withUsername("test@example.com").password("unused").authorities("USER").build();
        when(jwtService.extractUsername("valid")).thenReturn(user.getUsername());
        when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        var failure = new ServletException("Upstream service failure");
        FilterChain chain = (req, res) -> {
            assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            throw failure;
        };

        assertSame(failure, assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain)));
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    void unexpectedAuthenticationInfrastructureErrorPropagates() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractUsername("token")).thenThrow(new IllegalStateException("Configuration failure"));

        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class)));
    }
}
