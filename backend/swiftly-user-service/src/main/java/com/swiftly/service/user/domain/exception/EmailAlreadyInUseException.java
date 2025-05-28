package com.swiftly.service.user.domain.exception;

public class EmailAlreadyInUseException extends RuntimeException {
  public EmailAlreadyInUseException(String message) {
    super(message);
  }
}
