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

    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        try {
            AuthorizationRequest request = new AuthorizationRequest();
            request.setCode(code);
            request.setState(state);
            log.info("Received callback - code: {}, state: {}", code, state);

            GarminUserTokens garminUserTokens = garminService.exchangeCodeForToken(request);
            log.info("Successfully exchanged code for token for user: {}", garminUserTokens.getId().getUserId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Garmin account connected successfully",
                "userId", garminUserTokens.getId().getUserId(),
                "garminUserId", garminUserTokens.getId().getGarminUserId()
            ));
        } catch (Exception e) {
            log.error("Error during Garmin callback handling", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
