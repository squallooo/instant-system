package com.ahallili.instant_system.parkings.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ahallili.instant_system.parkings.conf.SecurityErrorHandler;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class ControllerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(InvalidLocationException.class)
    public ResponseEntity<String> handleInvalidLocation(InvalidLocationException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(exception = { AuthenticationException.class, JwtException.class })
    public ResponseEntity<String> HandleLoginUnauthorizedException(Exception exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Bearer")
                .contentType(MediaType.TEXT_PLAIN)
                .body(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN)
                .body(SecurityErrorHandler.FORBIDDEN_MESSAGE);
    }

    @ExceptionHandler(exception = { HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<String> HandleLoginBadRequestException(Exception exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("wrong or bad Parameter please check your request");
    }

    @ExceptionHandler(exception = { ParkingNotFoundException.class })
    public ResponseEntity<String> HandleLoginNotFoundException(Exception exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No result found");
    }

    @ExceptionHandler(exception = { EmailAlreadyInUseException.class })
    public ResponseEntity<String> HandleLoginEmailExistsException(Exception exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("The used Email already exists");
    }

    @ExceptionHandler(exception = { ClientApiTimeOutException.class })
    public ResponseEntity<String> HandleLoginTimeoutException(Exception exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body("the request took too long");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException exception) {
        logger.warn("", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal error. Please contact the admin if the issue remains");
    }

}
