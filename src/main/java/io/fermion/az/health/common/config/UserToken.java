package io.fermion.az.health.common.config;

/**
 * Interface representing user authentication token information
 */
public interface UserToken {
  /**
   * Get the user ID from the authentication token
   * 
   * @return the user ID as a String
   */
  String getUserId();

  /**
   * Get the token value
   * 
   * @return the token value as a String
   */
  String getToken();

  /**
   * Check if the token is valid
   * 
   * @return true if the token is valid, false otherwise
   */
  boolean isValid();
}