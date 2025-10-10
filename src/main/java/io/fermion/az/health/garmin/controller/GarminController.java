package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.dto.DailiesSummary;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/garmin")
@RequiredArgsConstructor
public class GarminController {

  private final GarminService garminService;

  // Auth endpoints - FIXED: Added userId parameter
  @GetMapping("/auth")
  public ResponseEntity<String> initiateGarminAuth(@RequestParam String userId) {
    String authUrl = garminService.generateAuthorizationUrl(userId);
    return ResponseEntity.ok(authUrl);
  }

  @GetMapping("/auth/callback")
  public ResponseEntity<String> garminCallback(
      @RequestParam String code,
      @RequestParam String state) {
    GarminUserTokens tokens = garminService.handleOAuthCallback(code, state);
    return ResponseEntity.ok("Authentication successful! User ID: " + tokens.getId().getUserId());
  }

  // Existing endpoints
  @PostMapping("/token")
  public ResponseEntity<GarminUserTokens> exchangeToken(@RequestBody AuthorizationRequest request) {
    GarminUserTokens tokens = garminService.exchangeCodeForToken(request);
    return ResponseEntity.ok(tokens);
  }

  @GetMapping("/dailies/today")
  public ResponseEntity<DailiesSummary[]> getTodayDailies(@RequestHeader("Authorization") String accessToken) {
    String token = accessToken.replace("Bearer ", "");
    DailiesSummary[] dailies = garminService.getTodayDailiesSummary(token);
    return ResponseEntity.ok(dailies);
  }

  @GetMapping("/dailies")
  public ResponseEntity<DailiesSummary[]> getDailiesByDate(
      @RequestHeader("Authorization") String accessToken,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    String token = accessToken.replace("Bearer ", "");
    DailiesSummary[] dailies = garminService.getDailiesSummary(token, date);
    return ResponseEntity.ok(dailies);
  }

  // Health check endpoint to verify deployment
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Garmin Service is running!");
  }
}
