package com.ahallili.instant_system.parkings.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahallili.instant_system.parkings.auth.User;

public interface UserRepository extends JpaRepository<User, Long> {

    public Optional<User> findByEmail(String email);

    public boolean existsByEmail(String email);

}
