package com.msa4meerkatgram.global.errors.custom;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
