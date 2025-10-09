package io.fermion.az.health.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailiesSummary {
  private String userId;
  private String summaryId;
  private String calendarDate;
  private String activityType;
  private Integer activeKilocalories;
  private Integer bmrKilocalories;
  private Integer steps;
  private Integer pushes;
  private Double distanceInMeters;
  private Double pushDistanceInMeters;
  private Long durationInSeconds;
  private Long activeTimeInSeconds;
  private Long startTimeInSeconds;
  private Long startTimeOffsetInSeconds;
  private Long moderateIntensityDurationInSeconds;
  private Long vigorousIntensityDurationInSeconds;
  private Integer floorsClimbed;
  private Integer minHeartRateInBeatsPerMinute;
  private Integer maxHeartRateInBeatsPerMinute;
  private Integer averageHeartRateInBeatsPerMinute;
  private Integer restingHeartRateInBeatsPerMinute;
  @JsonProperty("timeOffsetHeartRateSamples")
  private String timeOffsetHeartRateSamples;
  private String source;
  private Integer stepsGoal;
  private Integer pushesGoal;
  private Long intensityDurationGoalInSeconds;
  private Integer floorsClimbedGoal;
  private Integer averageStressLevel;
  private Integer maxStressLevel;
  private Long stressDurationInSeconds;
  private Long restStressDurationInSeconds;
  private Long activityStressDurationInSeconds;
  private Long lowStressDurationInSeconds;
  private Long mediumStressDurationInSeconds;
  private Long highStressDurationInSeconds;
  private String stressQualifier;
  private Integer bodyBatteryChargedValue;
  private Integer bodyBatteryDrainedValue;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSummaryId() {
    return summaryId;
  }

  public void setSummaryId(String summaryId) {
    this.summaryId = summaryId;
  }

  public String getCalendarDate() {
    return calendarDate;
  }

  public void setCalendarDate(String calendarDate) {
    this.calendarDate = calendarDate;
  }

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public Integer getActiveKilocalories() {
    return activeKilocalories;
  }

  public void setActiveKilocalories(Integer activeKilocalories) {
    this.activeKilocalories = activeKilocalories;
  }

  public Integer getBmrKilocalories() {
    return bmrKilocalories;
  }

  public void setBmrKilocalories(Integer bmrKilocalories) {
    this.bmrKilocalories = bmrKilocalories;
  }

  public Integer getSteps() {
    return steps;
  }

  public void setSteps(Integer steps) {
    this.steps = steps;
  }

  public Integer getPushes() {
    return pushes;
  }

  public void setPushes(Integer pushes) {
    this.pushes = pushes;
  }

  public Double getDistanceInMeters() {
    return distanceInMeters;
  }

  public void setDistanceInMeters(Double distanceInMeters) {
    this.distanceInMeters = distanceInMeters;
  }

  public Double getPushDistanceInMeters() {
    return pushDistanceInMeters;
  }

  public void setPushDistanceInMeters(Double pushDistanceInMeters) {
    this.pushDistanceInMeters = pushDistanceInMeters;
  }

  public Long getDurationInSeconds() {
    return durationInSeconds;
  }

  public void setDurationInSeconds(Long durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  public Long getActiveTimeInSeconds() {
    return activeTimeInSeconds;
  }

  public void setActiveTimeInSeconds(Long activeTimeInSeconds) {
    this.activeTimeInSeconds = activeTimeInSeconds;
  }

  public Long getStartTimeInSeconds() {
    return startTimeInSeconds;
  }

  public void setStartTimeInSeconds(Long startTimeInSeconds) {
    this.startTimeInSeconds = startTimeInSeconds;
  }

  public Long getStartTimeOffsetInSeconds() {
    return startTimeOffsetInSeconds;
  }

  public void setStartTimeOffsetInSeconds(Long startTimeOffsetInSeconds) {
    this.startTimeOffsetInSeconds = startTimeOffsetInSeconds;
  }

  public Long getModerateIntensityDurationInSeconds() {
    return moderateIntensityDurationInSeconds;
  }

  public void setModerateIntensityDurationInSeconds(Long moderateIntensityDurationInSeconds) {
    this.moderateIntensityDurationInSeconds = moderateIntensityDurationInSeconds;
  }

  public Long getVigorousIntensityDurationInSeconds() {
    return vigorousIntensityDurationInSeconds;
  }

  public void setVigorousIntensityDurationInSeconds(Long vigorousIntensityDurationInSeconds) {
    this.vigorousIntensityDurationInSeconds = vigorousIntensityDurationInSeconds;
  }

  public Integer getFloorsClimbed() {
    return floorsClimbed;
  }

  public void setFloorsClimbed(Integer floorsClimbed) {
    this.floorsClimbed = floorsClimbed;
  }

  public Integer getMinHeartRateInBeatsPerMinute() {
    return minHeartRateInBeatsPerMinute;
  }

  public void setMinHeartRateInBeatsPerMinute(Integer minHeartRateInBeatsPerMinute) {
    this.minHeartRateInBeatsPerMinute = minHeartRateInBeatsPerMinute;
  }

  public Integer getMaxHeartRateInBeatsPerMinute() {
    return maxHeartRateInBeatsPerMinute;
  }

  public void setMaxHeartRateInBeatsPerMinute(Integer maxHeartRateInBeatsPerMinute) {
    this.maxHeartRateInBeatsPerMinute = maxHeartRateInBeatsPerMinute;
  }

  public Integer getAverageHeartRateInBeatsPerMinute() {
    return averageHeartRateInBeatsPerMinute;
  }

  public void setAverageHeartRateInBeatsPerMinute(Integer averageHeartRateInBeatsPerMinute) {
    this.averageHeartRateInBeatsPerMinute = averageHeartRateInBeatsPerMinute;
  }

  public Integer getRestingHeartRateInBeatsPerMinute() {
    return restingHeartRateInBeatsPerMinute;
  }

  public void setRestingHeartRateInBeatsPerMinute(Integer restingHeartRateInBeatsPerMinute) {
    this.restingHeartRateInBeatsPerMinute = restingHeartRateInBeatsPerMinute;
  }

  public String getTimeOffsetHeartRateSamples() {
    return timeOffsetHeartRateSamples;
  }

  public void setTimeOffsetHeartRateSamples(String timeOffsetHeartRateSamples) {
    this.timeOffsetHeartRateSamples = timeOffsetHeartRateSamples;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Integer getStepsGoal() {
    return stepsGoal;
  }

  public void setStepsGoal(Integer stepsGoal) {
    this.stepsGoal = stepsGoal;
  }

  public Integer getPushesGoal() {
    return pushesGoal;
  }

  public void setPushesGoal(Integer pushesGoal) {
    this.pushesGoal = pushesGoal;
  }

  public Long getIntensityDurationGoalInSeconds() {
    return intensityDurationGoalInSeconds;
  }

  public void setIntensityDurationGoalInSeconds(Long intensityDurationGoalInSeconds) {
    this.intensityDurationGoalInSeconds = intensityDurationGoalInSeconds;
  }

  public Integer getFloorsClimbedGoal() {
    return floorsClimbedGoal;
  }

  public void setFloorsClimbedGoal(Integer floorsClimbedGoal) {
    this.floorsClimbedGoal = floorsClimbedGoal;
  }

  public Integer getAverageStressLevel() {
    return averageStressLevel;
  }

  public void setAverageStressLevel(Integer averageStressLevel) {
    this.averageStressLevel = averageStressLevel;
  }

  public Integer getMaxStressLevel() {
    return maxStressLevel;
  }

  public void setMaxStressLevel(Integer maxStressLevel) {
    this.maxStressLevel = maxStressLevel;
  }

  public Long getStressDurationInSeconds() {
    return stressDurationInSeconds;
  }

  public void setStressDurationInSeconds(Long stressDurationInSeconds) {
    this.stressDurationInSeconds = stressDurationInSeconds;
  }

  public Long getRestStressDurationInSeconds() {
    return restStressDurationInSeconds;
  }

  public void setRestStressDurationInSeconds(Long restStressDurationInSeconds) {
    this.restStressDurationInSeconds = restStressDurationInSeconds;
  }

  public Long getActivityStressDurationInSeconds() {
    return activityStressDurationInSeconds;
  }

  public void setActivityStressDurationInSeconds(Long activityStressDurationInSeconds) {
    this.activityStressDurationInSeconds = activityStressDurationInSeconds;
  }

  public Long getLowStressDurationInSeconds() {
    return lowStressDurationInSeconds;
  }

  public void setLowStressDurationInSeconds(Long lowStressDurationInSeconds) {
    this.lowStressDurationInSeconds = lowStressDurationInSeconds;
  }

  public Long getMediumStressDurationInSeconds() {
    return mediumStressDurationInSeconds;
  }

  public void setMediumStressDurationInSeconds(Long mediumStressDurationInSeconds) {
    this.mediumStressDurationInSeconds = mediumStressDurationInSeconds;
  }

  public Long getHighStressDurationInSeconds() {
    return highStressDurationInSeconds;
  }

  public void setHighStressDurationInSeconds(Long highStressDurationInSeconds) {
    this.highStressDurationInSeconds = highStressDurationInSeconds;
  }

  public String getStressQualifier() {
    return stressQualifier;
  }

  public void setStressQualifier(String stressQualifier) {
    this.stressQualifier = stressQualifier;
  }

  public Integer getBodyBatteryChargedValue() {
    return bodyBatteryChargedValue;
  }

  public void setBodyBatteryChargedValue(Integer bodyBatteryChargedValue) {
    this.bodyBatteryChargedValue = bodyBatteryChargedValue;
  }

  public Integer getBodyBatteryDrainedValue() {
    return bodyBatteryDrainedValue;
  }

  public void setBodyBatteryDrainedValue(Integer bodyBatteryDrainedValue) {
    this.bodyBatteryDrainedValue = bodyBatteryDrainedValue;
  }
}
