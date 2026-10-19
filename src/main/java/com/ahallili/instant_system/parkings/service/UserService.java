package com.ahallili.instant_system.parkings.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ahallili.instant_system.parkings.auth.User;
import com.ahallili.instant_system.parkings.exception.EmailAlreadyInUseException;
import com.ahallili.instant_system.parkings.repository.UserRepository;

@Service
public class UserService implements IUserService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    public UserService(UserRepository userRepository, AuthenticationManager authManager,
            AuthenticationManager authenticationManager, BCryptPasswordEncoder bcCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.bcryptPasswordEncoder = bcCryptPasswordEncoder;
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User createUser(String email, String name, String lastName, String pwd) {
        // crypt pwd
        String cryptedPwd = bcryptPasswordEncoder.encode(pwd);
        // save new user
        User user = new User(email, name, lastName, cryptedPwd);
        try {
            // Flush here so a database constraint failure is caught during signup.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new EmailAlreadyInUseException("Email already in use");
            }
            throw exception;
        }
        return user;
    }

    private boolean isEmailConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName().toLowerCase(Locale.ROOT);
                // H2 may report the backing index instead of the unique constraint name.
                if (name.matches("(?:[^.]+\\.)?uk_users_email(?:_index_[a-z0-9]+)?")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Optional<User> login(String email, String pws) throws AuthenticationException {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, pws));
        return userRepository.findByEmail(email);
    }

}
