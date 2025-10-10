package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.dto.DailiesSummary;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/garmin")
@RequiredArgsConstructor
public class GarminController {

  private final GarminService garminService;

  /**
   * Step 1: Initiate Garmin OAuth flow
   * This generates the authorization URL that redirects users to Garmin's consent page
   */
  @GetMapping("/auth")
  public ResponseEntity<Map<String, String>> initiateGarminAuth(@RequestParam String userId) {
    String authUrl = garminService.generateAuthorizationUrl(userId);
    
    Map<String, String> response = new HashMap<>();
    response.put("authorizationUrl", authUrl);
    response.put("userId", userId);
    response.put("instructions", "User must visit this URL to authorize Garmin Connect data access");
    
    return ResponseEntity.ok(response);
  }

  /**
   * Alternative: Direct redirect to Garmin authorization
   * Usage: GET /api/garmin/auth/redirect?userId=your-user-id
   */
  @GetMapping("/auth/redirect")
  public RedirectView redirectToGarminAuth(@RequestParam String userId) {
    String authUrl = garminService.generateAuthorizationUrl(userId);
    return new RedirectView(authUrl);
  }

  /**
   * Step 2: OAuth callback - Garmin redirects here after user authorization
   * This exchanges the authorization code for access tokens
   */
  @GetMapping("/auth/callback")
  public ResponseEntity<Map<String, Object>> garminCallback(
      @RequestParam String code,
      @RequestParam String state) {
    
    try {
      GarminUserTokens tokens = garminService.handleOAuthCallback(code, state);
      
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Successfully connected to Garmin Connect!");
      response.put("userId", tokens.getId().getUserId());
      response.put("garminUserId", tokens.getId().getGarminUserId());
      response.put("status", tokens.getConnectStatus());
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Check connection status for a user
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getConnectionStatus(@RequestParam String userId) {
    Map<String, Object> status = garminService.getConnectionStatus(userId);
    return ResponseEntity.ok(status);
  }

  // Existing endpoints
  @PostMapping("/token")
  public ResponseEntity<GarminUserTokens> exchangeToken(@RequestBody AuthorizationRequest request) {
    GarminUserTokens tokens = garminService.exchangeCodeForToken(request);
    return ResponseEntity.ok(tokens);
  }

  @GetMapping("/dailies/today")
  public ResponseEntity<DailiesSummary[]> getTodayDailies(
      @RequestParam String userId) {
    DailiesSummary[] dailies = garminService.getTodayDailiesSummaryForUser(userId);
    return ResponseEntity.ok(dailies);
  }

  @GetMapping("/dailies")
  public ResponseEntity<DailiesSummary[]> getDailiesByDate(
      @RequestParam String userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    DailiesSummary[] dailies = garminService.getDailiesSummaryForUser(userId, date);
    return ResponseEntity.ok(dailies);
  }

  // Health check endpoint
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> health = new HashMap<>();
    health.put("status", "UP");
    health.put("service", "Garmin Integration Service");
    return ResponseEntity.ok(health);
  }
}
