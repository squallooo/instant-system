package com.ahallili.instant_system.parkings.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ahallili.instant_system.parkings.auth.User;
import com.ahallili.instant_system.parkings.dto.AuthenticationRequestDto;
import com.ahallili.instant_system.parkings.dto.AuthenticationResponseDto;
import com.ahallili.instant_system.parkings.dto.ParkingDto;
import com.ahallili.instant_system.parkings.dto.UserRequestDto;
import com.ahallili.instant_system.parkings.dto.UserResponseDto;
import com.ahallili.instant_system.parkings.exception.EmailAlreadyInUseException;
import com.ahallili.instant_system.parkings.exception.ParkingNotFoundException;
import com.ahallili.instant_system.parkings.service.IUserService;
import com.ahallili.instant_system.parkings.service.JwtService;
import com.ahallili.instant_system.parkings.service.ParkingService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final JwtService jwtService;
    private final IUserService userService;
    private final ParkingService parkingService;
    private final Logger logger = LoggerFactory.getLogger(ApiController.class);

    ApiController(ParkingService parkingService, IUserService userService, JwtService jwtService) {
        this.parkingService = parkingService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        logger.info("Received request get /api/hello");
        return new ResponseEntity<>("service is running", HttpStatus.OK);
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<UserResponseDto> signup(@RequestBody UserRequestDto userRequest)
            throws EmailAlreadyInUseException {

        logger.info("Received request post /api/auth/signup");

        User user = userService.createUser(userRequest.email(), userRequest.name(), userRequest.lastName(),
                userRequest.pwd());

        return new ResponseEntity<>(
                new UserResponseDto(user.getEmail(), user.getName(), user.getLastName(), user.getCreationDate()),
                HttpStatus.OK);
    }

    @GetMapping("/user/users")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserResponseDto>> getUsers() {
        
        logger.info("Received request get /api/user/users");

        List<UserResponseDto> users = userService.getAll().stream()
                .map(u -> new UserResponseDto(u.getEmail(), u.getName(), u.getLastName(), u.getCreationDate()))
                .toList();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthenticationResponseDto> login(@RequestBody AuthenticationRequestDto request) {

        logger.info("Received request get /api/auth/login");

        AuthenticationResponseDto res = null;

        Optional<User> authenticatedUser = userService.login(request.email(), request.pwd());

        String jwtToken = jwtService.generateToken(authenticatedUser.get());

        res = new AuthenticationResponseDto(jwtToken, jwtService.getExpirationTime(), "USER");

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @DeleteMapping("/auth/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {

        logger.info("Received request get /api/auth/logout");

        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new BadCredentialsException("Missing token");
        }
        jwtService.revokeToken(authorization.substring(7));
        return new ResponseEntity<>("you are logged out", HttpStatus.OK);
    }

    @GetMapping("/available-cities")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<String>> getAvailableCities() {
        logger.info("Received request get /api/available-cities");
        return new ResponseEntity<>(
                parkingService.getSupportedCities().stream().toList(),
                HttpStatus.OK);
    }

    @GetMapping("/{city}/parkings")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ParkingDto>> getCityParkings(@PathVariable() String city)
            throws ParkingNotFoundException {
        logger.info("Received request get /api/{}/parkings", city);

        if (city == null || city.isEmpty()) {
            logger.warn("City parameter is missing or empty");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Check if the city is available in the configuration
        if (parkingService.getSupportedCities().stream()
                .noneMatch(c -> c.equalsIgnoreCase(city))) {
            logger.warn("City {} is not available", city);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Implementation for fetching parkings in a specific city
        return new ResponseEntity<>(parkingService.getParkings(city), HttpStatus.OK);
    }

    @GetMapping("/{city}/{parking_id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ParkingDto> getParkingDetails(@PathVariable() String parking_id,
            @PathVariable() String city) throws ParkingNotFoundException {

        logger.info("Received request get /api/{}/{}", city, parking_id);
        // Implementation for fetching details of a specific parking in a specific city
        return new ResponseEntity<>(parkingService.getParkingDetails(city, parking_id), HttpStatus.OK);
    }

    /**
     * f
     * 
     * @param city
     * @param geoPoint
     * @param radiusMeters in meter
     * @return
     */
    @GetMapping("/{city}/find_nearby_parkings")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ParkingDto>> getNearByParkings(@PathVariable String city, @RequestParam String geoPoint,
            @RequestParam int radiusMeters) throws ParkingNotFoundException {

        logger.info("recieved request get /api/{}/find_nearby_parkings", city);

        return new ResponseEntity<>(parkingService.getNearByParkings(city, geoPoint, radiusMeters), HttpStatus.OK);
    }

}
