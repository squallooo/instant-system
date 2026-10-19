package com.ahallili.instant_system.parkings.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ahallili.instant_system.parkings.auth.User;
import com.ahallili.instant_system.parkings.repository.UserRepository;

class UserServiceTest {
    @Test
    void doesNotHideOtherDatabaseConstraints() {
        var cause = new org.hibernate.exception.ConstraintViolationException("constraint",
                new java.sql.SQLException(), "other_constraint");
        var failure = new org.springframework.dao.DataIntegrityViolationException("Database error", cause);
        when(repository.saveAndFlush(any())).thenThrow(failure);

        assertSame(failure, assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.createUser("test@example.com", "Test", "User", "password")));
    }

    private final UserRepository repository = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserService service = new UserService(repository, authenticationManager, authenticationManager, encoder);

    @Test
    void savesHashedPasswordAtSignup() {
        User user = service.createUser("test@example.com", "Amine", "Test", "password");

        assertNotEquals("password", user.getPassword());
        assertTrue(encoder.matches("password", user.getPassword()));
        assertEquals("test@example.com", user.getEmail());
        verify(repository).saveAndFlush(user);
    }

    @Test
    void returnsUserAfterAuthentication() {
        User user = new User("test@example.com", "Amine", "Test", "hash");
        var credentials = new UsernamePasswordAuthenticationToken("test@example.com", "password");
        when(authenticationManager.authenticate(credentials)).thenReturn(credentials);
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertEquals(Optional.of(user), service.login("test@example.com", "password"));
        verify(authenticationManager).authenticate(credentials);
    }

    @Test
    void stopsWhenCredentialsAreIncorrect() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> service.login("test@example.com", "wrong"));
        verifyNoInteractions(repository);
    }
}
