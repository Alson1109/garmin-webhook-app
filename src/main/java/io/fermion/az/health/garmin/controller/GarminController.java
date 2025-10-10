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

  @PostMapping("/token")
  public ResponseEntity<GarminUserTokens> exchangeToken(@RequestBody AuthorizationRequest request) {
    GarminUserTokens tokens = garminService.exchangeCodeForToken(request);
    return ResponseEntity.ok(tokens);
  }

  @GetMapping("/dailies/today")
  public ResponseEntity<DailiesSummary[]> getTodayDailies(@RequestHeader("Authorization") String accessToken) {
    // Remove "Bearer " prefix if present
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