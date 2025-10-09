package io.fermion.az.health.garmin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "garmin_dailies_summary")
@Data
public class GarminUserDailiesSummary {
  @EmbeddedId
  private GarminDailiesSummaryId id;

  @Column(name = "summary_id")
  private String summaryId;

  @Column(name = "activity_type", length = 50)
  private String activityType;

  // Calorie and Activity Metrics
  @Column(name = "active_kilocalories")
  private Integer activeKilocalories = 0;

  @Column(name = "bmr_kilocalories")
  private Integer bmrKilocalories = 0;

  @Column(name = "steps")
  private Integer steps = 0;

  @Column(name = "pushes")
  private Integer pushes = 0;

  @Column(name = "distance_in_meters")
  private Double distanceInMeters = 0.0;

  @Column(name = "push_distance_in_meters")
  private Double pushDistanceInMeters = 0.0;

  // Time Metrics (in seconds)
  @Column(name = "duration_in_seconds")
  private Long durationInSeconds = 0L;

  @Column(name = "active_time_in_seconds")
  private Long activeTimeInSeconds = 0L;

  @Column(name = "start_time_in_seconds")
  private Long startTimeInSeconds;

  @Column(name = "start_time_offset_in_seconds")
  private Long startTimeOffsetInSeconds;

  @Column(name = "moderate_intensity_duration_in_seconds")
  private Long moderateIntensityDurationInSeconds = 0L;

  @Column(name = "vigorous_intensity_duration_in_seconds")
  private Long vigorousIntensityDurationInSeconds = 0L;

  // Physical Activity
  @Column(name = "floors_climbed")
  private Integer floorsClimbed = 0;

  // Heart Rate Metrics
  @Column(name = "min_heart_rate_in_beats_per_minute")
  private Integer minHeartRateInBeatsPerMinute;

  @Column(name = "max_heart_rate_in_beats_per_minute")
  private Integer maxHeartRateInBeatsPerMinute;

  @Column(name = "average_heart_rate_in_beats_per_minute")
  private Integer averageHeartRateInBeatsPerMinute;

  @Column(name = "resting_heart_rate_in_beats_per_minute")
  private Integer restingHeartRateInBeatsPerMinute;

  @Column(name = "time_offset_heart_rate_samples", columnDefinition = "JSON")
  private String timeOffsetHeartRateSamples;

  @Column(name = "source", length = 100)
  private String source;

  // Goals
  @Column(name = "steps_goal")
  private Integer stepsGoal;

  @Column(name = "pushes_goal")
  private Integer pushesGoal;

  @Column(name = "intensity_duration_goal_in_seconds")
  private Long intensityDurationGoalInSeconds;

  @Column(name = "floors_climbed_goal")
  private Integer floorsClimbedGoal;

  // Stress Metrics
  @Column(name = "average_stress_level")
  private Integer averageStressLevel;

  @Column(name = "max_stress_level")
  private Integer maxStressLevel;

  @Column(name = "stress_duration_in_seconds")
  private Long stressDurationInSeconds = 0L;

  @Column(name = "rest_stress_duration_in_seconds")
  private Long restStressDurationInSeconds = 0L;

  @Column(name = "activity_stress_duration_in_seconds")
  private Long activityStressDurationInSeconds = 0L;

  @Column(name = "low_stress_duration_in_seconds")
  private Long lowStressDurationInSeconds = 0L;

  @Column(name = "medium_stress_duration_in_seconds")
  private Long mediumStressDurationInSeconds = 0L;

  @Column(name = "high_stress_duration_in_seconds")
  private Long highStressDurationInSeconds = 0L;

  @Column(name = "stress_qualifier", length = 50)
  private String stressQualifier;

  // Body Battery
  @Column(name = "body_battery_charged_value")
  private Integer bodyBatteryChargedValue;

  @Column(name = "body_battery_drained_value")
  private Integer bodyBatteryDrainedValue;

  // Metadata
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "last_modified_at")
  private LocalDateTime lastModifiedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    lastModifiedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    lastModifiedAt = LocalDateTime.now();
  }
}
