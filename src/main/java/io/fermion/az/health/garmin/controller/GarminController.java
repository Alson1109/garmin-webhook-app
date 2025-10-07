package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.common.config.UserToken;
import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/garmin")
@Tag(name = "[Garmin] Integration")
@AllArgsConstructor
@Slf4j
public class GarminController {

  private final GarminService garminService;

  @Operation(summary = "Generate Garmin authorization URL")
  @ApiResponse(responseCode = "200", description = "Returns Garmin authorization URL")
  @GetMapping("/authorize")
  public ResponseEntity<Map<String, String>> generateAuthorizationUrl(
      UserToken userToken,
      @RequestParam(value = "testUserId", required = false) String testUserId) {

    String debiUserId;

    // Check if we're in test mode (testUserId provided) or real user mode
    if (testUserId != null && !testUserId.isEmpty()) {
      // Testing mode: use provided testUserId
      debiUserId = testUserId;
      log.info("Using test mode with user ID: {}", debiUserId);
    } else if (userToken != null && userToken.getUserId() != null) {
      // Production mode: use real authenticated Debi user
      debiUserId = userToken.getUserId();
      log.info("Using authenticated Debi user ID: {}", debiUserId);
    } else {
      // Fallback for testing when no auth token provided
      debiUserId = "fallback-user-" + System.currentTimeMillis();
      log.warn("No user authentication found, using fallback user ID: {}", debiUserId);
    }

    try {
      String authUrl = garminService.generateAuthorizationUrl(debiUserId);
      return ResponseEntity.ok(Map.of(
          "authorizationUrl", authUrl,
          "userId", debiUserId,
          "message", "Visit the authorization URL to connect your Garmin account to your Debi account"));
    } catch (Exception e) {
      log.error("Error generating authorization URL for user {}: {}", debiUserId, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to generate authorization URL: " + e.getMessage()));
    }
  }

  @Operation(summary = "Handle Garmin callback")
  @ApiResponse(responseCode = "200", description = "Callback from Garmin with authorization code")
  @GetMapping("/callback")
  public ResponseEntity<?> handleCallback(
      UserToken userToken,
      @RequestParam("code") String code,
      @RequestParam("state") String state) {
    try {
      AuthorizationRequest request = new AuthorizationRequest();
      request.setCode(code);
      request.setState(state);
      log.info("Received code: {}, state: {}", code, state);

      GarminUserTokens garminUserTokens = garminService.exchangeCodeForToken(request);
      log.info("Successfully exchanged code for token for user: {}", garminUserTokens.getId().getUserId());
      return ResponseEntity.ok(garminUserTokens);
    } catch (Exception e) {
      log.error("Error during Garmin callback handling", e);
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getMessage(),
              "type", e.getClass().getSimpleName()));
    }
  }

  @Operation(summary = "Fetch Garmin daily summaries")
  @ApiResponse(responseCode = "200", description = "Fetches daily summaries for a user")
  @GetMapping("/daily-summaries")
  public ResponseEntity<io.fermion.az.health.garmin.dto.DailiesSummary[]> fetchDailiesSummary(
      UserToken userToken,
      @RequestParam(required = false) Long uploadStartTimeInSeconds,
      @RequestParam(required = false) Long uploadEndTimeInSeconds) {

    String userId = userToken.getUserId();
    log.info("Fetching daily summaries for user: {}", userId);

    if (uploadStartTimeInSeconds == null || uploadEndTimeInSeconds == null) {
      java.time.LocalDate today = java.time.LocalDate.now();
      java.time.LocalDateTime startOfDay = today.atStartOfDay();
      uploadStartTimeInSeconds = startOfDay.toEpochSecond(java.time.ZoneOffset.UTC);
      java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
      uploadEndTimeInSeconds = endOfDay.toEpochSecond(java.time.ZoneOffset.UTC);
      log.info("Using default time range for user {}: {} to {}", userId, startOfDay, endOfDay);
    }

    try {
      var summaries = garminService.fetchDailiesData(userId, uploadStartTimeInSeconds, uploadEndTimeInSeconds);
      if (summaries == null) {
        log.warn("No daily summaries found for user: {}", userId);
        return ResponseEntity.ok(new io.fermion.az.health.garmin.dto.DailiesSummary[0]);
      }
      log.info("Found {} daily summaries for user: {}", summaries.length, userId);
      return ResponseEntity.ok(summaries);
    } catch (Exception e) {
      log.error("Error fetching daily summaries for user {}: {}", userId, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new io.fermion.az.health.garmin.dto.DailiesSummary[0]);
    }
  }

  @Operation(summary = "Fetch Garmin user permissions")
  @ApiResponse(responseCode = "200", description = "Fetches user permissions from Garmin")
  @GetMapping("/permissions")
  public ResponseEntity<List<String>> fetchUserPermissions(UserToken userToken) {
    List<String> permissions = garminService.fetchUserPermissions(userToken.getUserId());
    log.info("Fetched permissions for user {}: {}", userToken.getUserId(), permissions);
    return ResponseEntity.ok(permissions);
  }

  @Operation(summary = "Handle Garmin dailies push")
  @ApiResponse(responseCode = "200", description = "Successfully processed dailies push")
  @PostMapping("/dailies")
  public ResponseEntity<Map<String, Object>> handleDailiesPush(@RequestBody String pushData) {
    log.info("Received dailies push from Garmin: {}", pushData);
    try {
      garminService.processDailiesPush(pushData);
      return ResponseEntity.ok(Map.of(
          "status", "success",
          "message", "Dailies push processed successfully",
          "timestamp", System.currentTimeMillis()));
    } catch (Exception e) {
      log.error("Error processing dailies push: {}", e.getMessage(), e);
      // Still return 200 OK to prevent Garmin from retrying
      return ResponseEntity.ok(Map.of(
          "status", "error",
          "message", "Dailies push processing failed: " + e.getMessage(),
          "timestamp", System.currentTimeMillis()));
    }
  }

  @Operation(summary = "Register webhook for user")
  @ApiResponse(responseCode = "200", description = "Registers webhook to receive Garmin data pushes")
  @PostMapping("/webhook/register")
  public ResponseEntity<Map<String, Object>> registerWebhook(
      @RequestParam(value = "testUserId", required = false) String testUserId) {
    try {
      String userId = testUserId != null ? testUserId : "unknown";
      log.info("Attempting to register webhook for user: {}", userId);

      Map<String, Object> result = garminService.registerWebhook(userId);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Error registering webhook: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to register webhook: " + e.getMessage()));
    }
  }

  @Operation(summary = "Test webhook endpoint")
  @ApiResponse(responseCode = "200", description = "Test webhook endpoint for debugging")
  @PostMapping("/webhook/test")
  public ResponseEntity<Map<String, Object>> testWebhook(@RequestBody String testData) {
    log.info("Received test webhook data: {}", testData);
    return ResponseEntity.ok(Map.of(
        "message", "Webhook test successful",
        "receivedData", testData,
        "timestamp", System.currentTimeMillis()));
  }

  @Operation(summary = "Alternative dailies webhook endpoint")
  @ApiResponse(responseCode = "200", description = "Alternative endpoint for Garmin dailies webhooks")
  @PostMapping("/webhooks/dailies")
  public ResponseEntity<Void> handleDailiesWebhook(@RequestBody String pushData) {
    log.info("Received dailies webhook from Garmin: {}", pushData);
    garminService.processDailiesPush(pushData);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Generic Garmin webhook handler")
  @ApiResponse(responseCode = "200", description = "Generic webhook handler for any Garmin data")
  @PostMapping("/webhooks/garmin")
  public ResponseEntity<Map<String, Object>> handleGarminWebhook(@RequestBody String pushData) {
    log.info("Received generic Garmin webhook: {}", pushData);

    try {
      garminService.processDailiesPush(pushData);
      return ResponseEntity.ok(Map.of(
          "status", "success",
          "message", "Webhook processed successfully",
          "timestamp", System.currentTimeMillis()));
    } catch (Exception e) {
      log.error("Error processing Garmin webhook: {}", e.getMessage());
      return ResponseEntity.ok(Map.of(
          "status", "error",
          "message", "Webhook processing failed: " + e.getMessage(),
          "timestamp", System.currentTimeMillis()));
    }
  }

  @Operation(summary = "Get recent step data from stored webhooks")
  @ApiResponse(responseCode = "200", description = "Returns recently received step data from webhooks")
  @GetMapping("/webhook-data")
  public ResponseEntity<Map<String, Object>> getWebhookData(
      @RequestParam(value = "testUserId", required = false) String testUserId) {
    try {
      String userId = testUserId != null ? testUserId : "unknown";
      Map<String, Object> result = garminService.getRecentWebhookData(userId);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Error getting webhook data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to get webhook data: " + e.getMessage()));
    }
  }

  @Operation(summary = "Handle Garmin deregistration notification")
  @ApiResponse(responseCode = "200", description = "Handles deregistration notification from Garmin")
  @PostMapping("/notifications/deregistration")
  public ResponseEntity<Void> handleDeregistrationNotification(
      @RequestBody Map<String, List<Map<String, String>>> notification) {
    try {
      List<Map<String, String>> deregistrations = notification.get("deregistrations");
      if (deregistrations == null || deregistrations.isEmpty()) {
        log.warn("Received empty or invalid deregistration notification: {}", notification);
        return ResponseEntity.ok().build();
      }

      for (Map<String, String> dereg : deregistrations) {
        String garminUserId = dereg.get("userId");
        if (garminUserId != null) {
          garminService.handleDeregistration(garminUserId);
          log.info("Processed deregistration for Garmin user ID: {}", garminUserId);
        }
      }
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Error processing deregistration notification: {}", notification, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(summary = "Deregister Garmin user")
  @ApiResponse(responseCode = "204", description = "Deregisters a user from Garmin")
  @DeleteMapping("/deregister/{userId}")
  public ResponseEntity<Void> deregisterUser(@PathVariable String userId) {
    garminService.deregisterUser(userId);
    log.info("Deregistered user: {}", userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Check user token status")
  @ApiResponse(responseCode = "200", description = "Returns token status for debugging")
  @GetMapping("/token-status")
  public ResponseEntity<Map<String, Object>> checkTokenStatus(
      @RequestParam(value = "testUserId", required = false) String testUserId) {
    try {
      String userId = testUserId != null ? testUserId : "unknown";

      // Check if user has connected account
      boolean hasAccount = garminService.hasConnectedAccount(userId);

      Map<String, Object> status = Map.of(
          "userId", userId,
          "hasConnectedAccount", hasAccount,
          "message", hasAccount ? "User has connected Garmin account" : "No connected Garmin account found");

      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error checking token status: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to check token status: " + e.getMessage()));
    }
  }

  @Operation(summary = "Check OAuth state")
  @ApiResponse(responseCode = "200", description = "Returns OAuth state for debugging")
  @GetMapping("/check-state")
  public ResponseEntity<Map<String, Object>> checkState(
      @RequestParam("state") String state) {
    try {
      boolean stateExists = garminService.checkStateExists(state);

      Map<String, Object> status = Map.of(
          "state", state,
          "exists", stateExists,
          "message", stateExists ? "State found in database" : "State not found in database");

      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error checking state: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to check state: " + e.getMessage()));
    }
  }

  @Operation(summary = "Test Garmin API connection")
  @ApiResponse(responseCode = "200", description = "Tests basic Garmin API connection")
  @GetMapping("/test-garmin")
  public ResponseEntity<Map<String, Object>> testGarminConnection(
      @RequestParam(value = "testUserId", required = false) String testUserId) {
    try {
      String userId = testUserId != null ? testUserId : "unknown";

      // Step 1: Check if user has tokens
      boolean hasAccount = garminService.hasConnectedAccount(userId);
      if (!hasAccount) {
        return ResponseEntity.ok(Map.of(
            "step", "1",
            "error", "No connected Garmin account found",
            "userId", userId));
      }

      // Step 2: Try to get user token
      Map<String, Object> result = garminService.testConnection(userId);
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("Error testing Garmin connection: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to test Garmin connection: " + e.getMessage()));
    }
  }

  @Operation(summary = "Get step data for user")
  @ApiResponse(responseCode = "200", description = "Returns step data for the specified user and date range")
  @GetMapping("/steps")
  public ResponseEntity<Map<String, Object>> getStepData(
      UserToken userToken,
      @RequestParam(value = "testUserId", required = false) String testUserId,
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate) {

    try {
      // Use real authenticated user or test user
      String userId;
      if (testUserId != null && !testUserId.isEmpty()) {
        userId = testUserId;
        log.info("Fetching step data for test user: {}", userId);
      } else if (userToken != null && userToken.getUserId() != null) {
        userId = userToken.getUserId();
        log.info("Fetching step data for authenticated Debi user: {}", userId);
      } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error",
                "No user authentication provided. Please provide testUserId parameter for testing or ensure you're authenticated."));
      }

      // Set default date range (today) if not provided
      java.time.LocalDate start = (startDate != null) ? java.time.LocalDate.parse(startDate)
          : java.time.LocalDate.now();
      java.time.LocalDate end = (endDate != null) ? java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

      Long startSeconds = start.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
      Long endSeconds = end.atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC);

      var summaries = garminService.fetchDailiesData(userId, startSeconds, endSeconds);

      if (summaries == null || summaries.length == 0) {
        return ResponseEntity.ok(Map.of(
            "message", "No step data found for the specified period",
            "userId", userId,
            "dateRange", startDate + " to " + endDate,
            "totalSteps", 0,
            "data", new Object[] {}));
      }

      // Extract step data
      int totalSteps = 0;
      java.util.List<Map<String, Object>> stepData = new java.util.ArrayList<>();

      for (var summary : summaries) {
        int steps = (summary.getSteps() != null) ? summary.getSteps() : 0;
        totalSteps += steps;

        stepData.add(Map.of(
            "date", summary.getCalendarDate(),
            "steps", steps,
            "stepsGoal", (summary.getStepsGoal() != null) ? summary.getStepsGoal() : 0,
            "distanceInMeters", (summary.getDistanceInMeters() != null) ? summary.getDistanceInMeters() : 0.0,
            "activeKilocalories", (summary.getActiveKilocalories() != null) ? summary.getActiveKilocalories() : 0));
      }

      return ResponseEntity.ok(Map.of(
          "userId", userId,
          "dateRange", start + " to " + end,
          "totalSteps", totalSteps,
          "averageDailySteps", summaries.length > 0 ? totalSteps / summaries.length : 0,
          "data", stepData,
          "message", "Step data retrieved successfully"));

    } catch (Exception e) {
      log.error("Error fetching step data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to fetch step data: " + e.getMessage()));
    }
  }

  @Operation(summary = "Test callback for OAuth completion")
  @ApiResponse(responseCode = "200", description = "Test callback endpoint for OAuth testing")
  @GetMapping("/test-callback")
  public ResponseEntity<?> handleTestCallback(
      @RequestParam("code") String code,
      @RequestParam("state") String state,
      @RequestParam(value = "testUserId", required = false) String testUserId) {
    try {
      log.info("Test callback - Received code: {}, state: {}, testUserId: {}", code, state, testUserId);

      // Create the authorization request
      AuthorizationRequest request = new AuthorizationRequest();
      request.setCode(code);
      request.setState(state);

      // Exchange code for tokens
      GarminUserTokens garminUserTokens = garminService.exchangeCodeForToken(request);
      log.info("Successfully exchanged code for token for user: {}", garminUserTokens.getId().getUserId());

      return ResponseEntity.ok(Map.of(
          "message", "Garmin account connected successfully",
          "userId", garminUserTokens.getId().getUserId(),
          "garminUserId", garminUserTokens.getId().getGarminUserId(),
          "connectStatus", garminUserTokens.getConnectStatus().toString(),
          "tokenExpiry", garminUserTokens.getAccessTokenExpiry().toString()));
    } catch (Exception e) {
      log.error("Error during test callback handling", e);
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", "Failed to complete OAuth: " + e.getMessage(),
              "code", code,
              "state", state));
    }
  }

}
