package io.fermion.az.health.garmin.dto;

public class AuthorizationRequest {
  private String code;
  private String state;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
