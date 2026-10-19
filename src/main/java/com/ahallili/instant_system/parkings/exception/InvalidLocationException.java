package com.ahallili.instant_system.parkings.exception;

public class InvalidLocationException extends IllegalArgumentException {
    public InvalidLocationException(String message) {
        super(message);
    }
}
