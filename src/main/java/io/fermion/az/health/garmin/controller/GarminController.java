package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/garmin")
@AllArgsConstructor
@Slf4j
public class GarminController {

    private final GarminService garminService;

    // Simple test endpoint
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
            "message", "Garmin API is working!",
            "status", "success",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Garmin Data Fetch",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> generateAuthorizationUrl(
            @RequestParam(value = "testUserId", required = false) String testUserId) {

        String debiUserId = testUserId != null ? testUserId : "test-user-" + System.currentTimeMillis();
        log.info("Generating authorization URL for user: {}", debiUserId);

        try {
            String authUrl = garminService.generateAuthorizationUrl(debiUserId);
            return ResponseEntity.ok(Map.of(
                    "authorizationUrl", authUrl,
                    "userId", debiUserId,
                    "message", "Visit the authorization URL to connect your Garmin account"));
        } catch (Exception e) {
            log.error("Error generating authorization URL for user {}: {}", debiUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate authorization URL: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestBody(required = false) String body) {
        
        try {
            // If code/state are not in params, try to parse from body
            if (code == null && body != null) {
                log.info("Attempting to parse code from body: {}", body);
                // Parse code from form data or JSON body
                if (body.contains("code=")) {
                    String[] params = body.split("&");
                    for (String param : params) {
                        if (param.startsWith("code=")) {
                            code = param.substring(5);
                        } else if (param.startsWith("state=")) {
                            state = param.substring(6);
                        }
                    }
                }
            }

            if (code == null) {
                log.error("No authorization code received in callback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No authorization code provided"));
            }

            AuthorizationRequest request = new AuthorizationRequest();
            request.setCode(code);
            request.setState(state);
            log.info("Processing callback - code: {}, state: {}", code, state);

            GarminUserTokens garminUserTokens = garminService.exchangeCodeForToken(request);
            log.info("Successfully exchanged code for token for user: {}", garminUserTokens.getId().getUserId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Garmin account connected successfully!",
                "userId", garminUserTokens.getId().getUserId(),
                "garminUserId", garminUserTokens.getId().getGarminUserId(),
                "status", "connected"
            ));
        } catch (Exception e) {
            log.error("Error during Garmin callback handling", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to connect Garmin account: " + e.getMessage()));
        }
    }

    @GetMapping("/daily-summaries")
    public ResponseEntity<io.fermion.az.health.garmin.dto.DailiesSummary[]> fetchDailiesSummary(
            @RequestParam(value = "testUserId", required = false) String testUserId,
            @RequestParam(required = false) Long uploadStartTimeInSeconds,
            @RequestParam(required = false) Long uploadEndTimeInSeconds) {

        String userId = testUserId != null ? testUserId : "test-user-" + System.currentTimeMillis();
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
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Dailies push processing failed: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }
}
