package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.dto.DailiesSummary;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/garmin/webhook")
@RequiredArgsConstructor
public class GarminController {

  private static final Logger log = LoggerFactory.getLogger(GarminController.class);
  private final GarminService garminService;

  /**
   * Step 1: Generate authorization URL
   */
  @GetMapping("/auth")
  public ResponseEntity<Map<String, String>> initiateGarminAuth(
      @RequestParam String userId,
      HttpServletRequest request) {
    
    log.info("=== INITIATING GARMIN AUTH ===");
    log.info("User ID: {}", userId);
    
    String authUrl = garminService.generateAuthorizationUrl(userId);
    
    log.info("Generated Auth URL: {}", authUrl);
    
    Map<String, String> response = new HashMap<>();
    response.put("authorizationUrl", authUrl);
    response.put("userId", userId);
    response.put("message", "Visit this URL in your browser to authorize Garmin access");
    
    return ResponseEntity.ok(response);
  }

  /**
   * Step 2: OAuth callback - Exchange code for tokens AND fetch initial data
   */
  @GetMapping("/auth/callback")
  public ResponseEntity<Map<String, Object>> garminCallback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String error_description,
      HttpServletRequest request) {
    
    log.info("=== GARMIN CALLBACK RECEIVED ===");
    log.info("Request URL: {}", request.getRequestURL());
    log.info("Query String: {}", request.getQueryString());
    
    // Log all parameters
    Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      String paramValue = request.getParameter(paramName);
      log.info("Parameter: {} = {}", paramName, paramValue);
    }
    
    // Check for errors
    if (error != null) {
      log.error("OAuth Error: {} - {}", error, error_description);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", error);
      errorResponse.put("error_description", error_description);
      return ResponseEntity.badRequest().body(errorResponse);
    }
    
    // Validate parameters
    if (code == null || state == null) {
      log.error("Missing required parameters");
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", "Missing code or state parameter");
      return ResponseEntity.badRequest().body(errorResponse);
    }
    
    log.info("Authorization Code: {}", code);
    log.info("State: {}", state);
    
    try {
      // Exchange code for tokens
      log.info("=== EXCHANGING CODE FOR TOKENS ===");
      GarminUserTokens tokens = garminService.handleOAuthCallback(code, state);
      
      log.info("=== TOKEN EXCHANGE SUCCESSFUL ===");
      log.info("User ID: {}", tokens.getId().getUserId());
      log.info("Garmin User ID: {}", tokens.getId().getGarminUserId());
      
      // Immediately fetch today's health data
      log.info("=== FETCHING INITIAL HEALTH DATA ===");
      DailiesSummary[] healthData = null;
      try {
        healthData = garminService.getTodayDailiesSummaryForUser(tokens.getId().getUserId());
        log.info("Retrieved {} health summaries", healthData != null ? healthData.length : 0);
        
        // Log the data details
        if (healthData != null && healthData.length > 0) {
          for (DailiesSummary summary : healthData) {
            log.info("=== HEALTH DATA SUMMARY ===");
            log.info("Calendar Date: {}", summary.getCalendarDate());
            log.info("Steps: {}", summary.getSteps());
            log.info("Distance (meters): {}", summary.getDistanceInMeters());
            log.info("Active Calories: {}", summary.getActiveKilocalories());
            log.info("BMR Calories: {}", summary.getBmrKilocalories());
            log.info("Floors Climbed: {}", summary.getFloorsClimbed());
            log.info("Active Time (seconds): {}", summary.getActiveTimeInSeconds());
            log.info("Average Heart Rate: {}", summary.getAverageHeartRateInBeatsPerMinute());
            log.info("Resting Heart Rate: {}", summary.getRestingHeartRateInBeatsPerMinute());
          }
        }
      } catch (Exception e) {
        log.warn("Could not fetch initial health data: {}", e.getMessage());
      }
      
      // Build success response
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Successfully connected to Garmin Connect!");
      response.put("userId", tokens.getId().getUserId());
      response.put("garminUserId", tokens.getId().getGarminUserId());
      response.put("status", tokens.getConnectStatus());
      
      // Include health data if available
      if (healthData != null && healthData.length > 0) {
        response.put("initialHealthData", healthData);
        
        // Add a friendly summary
        DailiesSummary todayData = healthData[0];
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", todayData.getCalendarDate());
        summary.put("steps", todayData.getSteps());
        summary.put("distanceMeters", todayData.getDistanceInMeters());
        summary.put("activeCalories", todayData.getActiveKilocalories());
        summary.put("floorsClimbed", todayData.getFloorsClimbed());
        summary.put("averageHeartRate", todayData.getAverageHeartRateInBeatsPerMinute());
        
        response.put("todaySummary", summary);
      } else {
        response.put("note", "No health data available yet. Data will be synced from your Garmin device.");
      }
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("=== CALLBACK PROCESSING FAILED ===");
      log.error("Error: {}", e.getMessage(), e);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      errorResponse.put("errorType", e.getClass().getSimpleName());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Fetch today's health data for a user
   */
  @GetMapping("/data/today")
  public ResponseEntity<Map<String, Object>> getTodayData(@RequestParam String userId) {
    log.info("=== FETCHING TODAY'S DATA ===");
    log.info("User ID: {}", userId);
    
    try {
      DailiesSummary[] dailies = garminService.getTodayDailiesSummaryForUser(userId);
      
      Map<String, Object> response = new HashMap<>();
      
      if (dailies != null && dailies.length > 0) {
        DailiesSummary today = dailies[0];
        
        log.info("Steps: {}", today.getSteps());
        log.info("Distance: {} meters", today.getDistanceInMeters());
        log.info("Calories: {}", today.getActiveKilocalories());
        
        response.put("success", true);
        response.put("date", today.getCalendarDate());
        response.put("data", buildHealthDataMap(today));
        response.put("rawData", dailies);
      } else {
        response.put("success", false);
        response.put("message", "No data available for today");
      }
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("Error fetching data: {}", e.getMessage(), e);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Fetch health data for a specific date
   */
  @GetMapping("/data/date")
  public ResponseEntity<Map<String, Object>> getDataByDate(
      @RequestParam String userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    
    log.info("Fetching data for user: {} on date: {}", userId, date);
    
    try {
      DailiesSummary[] dailies = garminService.getDailiesSummaryForUser(userId, date);
      
      Map<String, Object> response = new HashMap<>();
      
      if (dailies != null && dailies.length > 0) {
        response.put("success", true);
        response.put("date", date);
        response.put("summaries", Arrays.stream(dailies)
            .map(this::buildHealthDataMap)
            .toArray());
        response.put("rawData", dailies);
      } else {
        response.put("success", false);
        response.put("message", "No data available for " + date);
      }
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("Error fetching data: {}", e.getMessage());
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Get connection status
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getConnectionStatus(@RequestParam String userId) {
    log.info("Checking connection status for user: {}", userId);
    Map<String, Object> status = garminService.getConnectionStatus(userId);
    return ResponseEntity.ok(status);
  }

    @GetMapping("/test-log")
  public ResponseEntity<Map<String, Object>> testLog(
      @RequestParam String userId,
      @RequestParam String accessToken,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    if (date == null) date = LocalDate.now();
    log.info("🧪 [TEST] Triggering Garmin daily summary log for user {} on {}", userId, date);

    try {
      garminService.logDailiesSummary(userId, date, accessToken);
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "✅ Garmin data fetch triggered — check Railway logs for output",
          "userId", userId,
          "date", date.toString()
      ));
    } catch (Exception e) {
      log.error("❌ Error during Garmin log test for user {}: {}", userId, e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  /**
   * Test endpoint
   */
  @GetMapping("/auth/callback/test")
  public ResponseEntity<Map<String, Object>> testCallback(HttpServletRequest request) {
    log.info("=== CALLBACK TEST ENDPOINT HIT ===");
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Callback endpoint is reachable");
    response.put("url", request.getRequestURL().toString());
    response.put("timestamp", System.currentTimeMillis());
    
    return ResponseEntity.ok(response);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> health = new HashMap<>();
    health.put("status", "UP");
    health.put("service", "Garmin Integration Service");
    return ResponseEntity.ok(health);
  }

  // Helper method to build structured health data
  private Map<String, Object> buildHealthDataMap(DailiesSummary summary) {
    Map<String, Object> data = new HashMap<>();
    
    // Basic info
    data.put("date", summary.getCalendarDate());
    data.put("activityType", summary.getActivityType());
    
    // Activity metrics
    Map<String, Object> activity = new HashMap<>();
    activity.put("steps", summary.getSteps());
    activity.put("distanceMeters", summary.getDistanceInMeters());
    activity.put("floorsClimbed", summary.getFloorsClimbed());
    activity.put("activeTimeSeconds", summary.getActiveTimeInSeconds());
    data.put("activity", activity);
    
    // Calories
    Map<String, Object> calories = new HashMap<>();
    calories.put("active", summary.getActiveKilocalories());
    calories.put("bmr", summary.getBmrKilocalories());
    calories.put("total", (summary.getActiveKilocalories() != null && summary.getBmrKilocalories() != null) 
        ? summary.getActiveKilocalories() + summary.getBmrKilocalories() : null);
    data.put("calories", calories);
    
    // Heart rate
    Map<String, Object> heartRate = new HashMap<>();
    heartRate.put("average", summary.getAverageHeartRateInBeatsPerMinute());
    heartRate.put("resting", summary.getRestingHeartRateInBeatsPerMinute());
    heartRate.put("min", summary.getMinHeartRateInBeatsPerMinute());
    heartRate.put("max", summary.getMaxHeartRateInBeatsPerMinute());
    data.put("heartRate", heartRate);
    
    // Stress
    if (summary.getAverageStressLevel() != null) {
      Map<String, Object> stress = new HashMap<>();
      stress.put("average", summary.getAverageStressLevel());
      stress.put("max", summary.getMaxStressLevel());
      stress.put("qualifier", summary.getStressQualifier());
      data.put("stress", stress);
    }
    
    // Body battery
    if (summary.getBodyBatteryChargedValue() != null) {
      Map<String, Object> bodyBattery = new HashMap<>();
      bodyBattery.put("charged", summary.getBodyBatteryChargedValue());
      bodyBattery.put("drained", summary.getBodyBatteryDrainedValue());
      data.put("bodyBattery", bodyBattery);
    }
    
    // Goals
    Map<String, Object> goals = new HashMap<>();
    goals.put("steps", summary.getStepsGoal());
    goals.put("floors", summary.getFloorsClimbedGoal());
    data.put("goals", goals);
    
    return data;
  }

  @PostMapping("/dailies")
  public ResponseEntity<Void> dailies(@RequestBody Map<String, Object> body,
                                      @RequestHeader Map<String, String> headers) {
    // Log headers + body so we can confirm Garmin is pushing
    log.info("📬 DAILIES webhook headers: {}", headers);
    log.info("📬 DAILIES webhook body: {}", body);
    return ResponseEntity.ok().build();
  }
}
