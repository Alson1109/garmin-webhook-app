package io.fermion.az.health.garmin.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GarminUserTokensId implements Serializable {

  private String userId;
  private String garminUserId;

  public GarminUserTokensId() {
  }

  public GarminUserTokensId(String userId, String garminUserId) {
    this.userId = userId;
    this.garminUserId = garminUserId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getGarminUserId() {
    return garminUserId;
  }

  public void setGarminUserId(String garminUserId) {
    this.garminUserId = garminUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GarminUserTokensId that = (GarminUserTokensId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(garminUserId, that.garminUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, garminUserId);
  }
}
