package io.fermion.az.health.garmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenResponse {
  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("refresh_token")
  private String refreshToken;
  @JsonProperty("token_type")
  private String tokenType;
  @JsonProperty("scope")
  private String scope;
  @JsonProperty("jti")
  private String jti;
  @JsonProperty("expires_in")
  private long expiresIn;
  @JsonProperty("refresh_token_expires_in")
  private long refreshTokenExpiresIn;
}
