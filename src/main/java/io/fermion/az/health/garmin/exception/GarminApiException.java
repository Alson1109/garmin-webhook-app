package io.fermion.az.health.garmin.exception;

public class GarminApiException extends RuntimeException {
  public GarminApiException(String message) {
    super(message);
  }

  public GarminApiException(String message, Throwable cause) {
    super(message, cause);
  }
}