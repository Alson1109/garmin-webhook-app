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

  // Auth endpoints
  @GetMapping("/auth")
  public String initiateGarminAuth() {
    String authUrl = garminService.generateAuthorizationUrl();
    return "redirect:" + authUrl;
  }

  @GetMapping("/auth/callback")
  public String garminCallback(@RequestParam String code,
      @RequestParam String state) {
    garminService.handleOAuthCallback(code, state);
    return "Authentication successful! You can close this window.";
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
}
