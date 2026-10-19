package com.ahallili.instant_system.parkings.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import com.ahallili.instant_system.parkings.exception.EmailAlreadyInUseException;
import com.ahallili.instant_system.parkings.exception.ControllerExceptionHandler;
import com.ahallili.instant_system.parkings.repository.UserRepository;

@SpringBootTest
class UserEmailConstraintTest {
    @Autowired private UserService service;
    @Autowired private UserRepository repository;

    @Test
    void databaseDuplicateBecomesConflict() {
        String email = UUID.randomUUID() + "@example.com";
        var user = service.createUser(email, "Test", "User", "password");
        try {
            var exception = assertThrows(EmailAlreadyInUseException.class,
                    () -> service.createUser(email, "Other", "User", "password"));
            assertEquals(HttpStatus.CONFLICT,
                    new ControllerExceptionHandler().HandleLoginEmailExistsException(exception).getStatusCode());
        } finally {
            repository.deleteById(user.getId());
        }
    }
}
