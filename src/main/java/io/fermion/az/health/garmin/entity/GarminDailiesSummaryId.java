package io.fermion.az.health.garmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class GarminDailiesSummaryId implements Serializable {

  @Column(name = "user_id", length = 36)
  private String userId;

  @Column(name = "garmin_user_id")
  private String garminUserId;

  @Column(name = "calendar_date")
  private LocalDate calendarDate;

  public GarminDailiesSummaryId() {
  }

  public GarminDailiesSummaryId(String userId, String garminUserId, LocalDate calendarDate) {
    this.userId = userId;
    this.garminUserId = garminUserId;
    this.calendarDate = calendarDate;
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

  public LocalDate getCalendarDate() {
    return calendarDate;
  }

  public void setCalendarDate(LocalDate calendarDate) {
    this.calendarDate = calendarDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GarminDailiesSummaryId that = (GarminDailiesSummaryId) o;
    return Objects.equals(userId, that.userId) &&
        Objects.equals(garminUserId, that.garminUserId) &&
        Objects.equals(calendarDate, that.calendarDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, garminUserId, calendarDate);
  }
}
