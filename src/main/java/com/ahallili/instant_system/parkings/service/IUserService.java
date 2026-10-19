package com.ahallili.instant_system.parkings.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.AuthenticationException;

import com.ahallili.instant_system.parkings.auth.User;
import com.ahallili.instant_system.parkings.exception.EmailAlreadyInUseException;

/**
 * 
 * i'm not using the dto as parameter as the user service could be used in other
 * appilications of the company
 * 
 * @param email
 * @param name
 * @param lastName
 * @return
 */
public interface IUserService {

    public User createUser(String email, String name, String lastName, String pwd) throws EmailAlreadyInUseException;

    public List<User> getAll();

    public Optional<User> login(String email, String pws) throws AuthenticationException;

}
