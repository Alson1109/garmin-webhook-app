package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.dto.DailiesSummary;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.service.GarminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/garmin")
@RequiredArgsConstructor
public class GarminController {

  private static final Logger log = LoggerFactory.getLogger(GarminController.class);
  private final GarminService garminService;

  /**
   * Step 1: Initiate Garmin OAuth flow
   */
  @GetMapping("/auth")
  public ResponseEntity<Map<String, String>> initiateGarminAuth(
      @RequestParam String userId,
      HttpServletRequest request) {
    
    log.info("=== INITIATING GARMIN AUTH ===");
    log.info("User ID: {}", userId);
    log.info("Request URL: {}", request.getRequestURL());
    
    String authUrl = garminService.generateAuthorizationUrl(userId);
    
    log.info("Generated Auth URL: {}", authUrl);
    
    Map<String, String> response = new HashMap<>();
    response.put("authorizationUrl", authUrl);
    response.put("userId", userId);
    response.put("instructions", "User must visit this URL to authorize Garmin Connect data access");
    
    return ResponseEntity.ok(response);
  }

  /**
   * Step 2: OAuth callback - THIS IS WHERE GARMIN SENDS DATA
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
    
    // Log ALL parameters
    log.info("--- All Request Parameters ---");
    Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      String paramValue = request.getParameter(paramName);
      log.info("Parameter: {} = {}", paramName, paramValue);
    }
    
    // Log ALL headers
    log.info("--- All Request Headers ---");
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      log.info("Header: {} = {}", headerName, headerValue);
    }
    
    // Check for errors first
    if (error != null) {
      log.error("OAuth Error received: {}", error);
      log.error("Error Description: {}", error_description);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", error);
      errorResponse.put("error_description", error_description);
      return ResponseEntity.badRequest().body(errorResponse);
    }
    
    // Validate required parameters
    if (code == null || state == null) {
      log.error("Missing required parameters - code: {}, state: {}", code, state);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", "Missing required parameters");
      errorResponse.put("received_code", code != null);
      errorResponse.put("received_state", state != null);
      return ResponseEntity.badRequest().body(errorResponse);
    }
    
    log.info("Authorization Code: {}", code);
    log.info("State: {}", state);
    
    try {
      GarminUserTokens tokens = garminService.handleOAuthCallback(code, state);
      
      log.info("=== TOKEN EXCHANGE SUCCESSFUL ===");
      log.info("User ID: {}", tokens.getId().getUserId());
      log.info("Garmin User ID: {}", tokens.getId().getGarminUserId());
      log.info("Status: {}", tokens.getConnectStatus());
      
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Successfully connected to Garmin Connect!");
      response.put("userId", tokens.getId().getUserId());
      response.put("garminUserId", tokens.getId().getGarminUserId());
      response.put("status", tokens.getConnectStatus());
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("=== TOKEN EXCHANGE FAILED ===");
      log.error("Error: {}", e.getMessage(), e);
      
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      errorResponse.put("errorType", e.getClass().getSimpleName());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Test endpoint to verify callback URL is reachable
   */
  @GetMapping("/auth/callback/test")
  public ResponseEntity<Map<String, Object>> testCallback(HttpServletRequest request) {
    log.info("=== CALLBACK TEST ENDPOINT HIT ===");
    log.info("Request URL: {}", request.getRequestURL());
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Callback endpoint is reachable");
    response.put("url", request.getRequestURL().toString());
    response.put("timestamp", System.currentTimeMillis());
    
    return ResponseEntity.ok(response);
  }

  /**
   * Check connection status
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getConnectionStatus(@RequestParam String userId) {
    log.info("Checking connection status for user: {}", userId);
    Map<String, Object> status = garminService.getConnectionStatus(userId);
    return ResponseEntity.ok(status);
  }

  @GetMapping("/dailies/today")
  public ResponseEntity<DailiesSummary[]> getTodayDailies(@RequestParam String userId) {
    log.info("Fetching today's dailies for user: {}", userId);
    DailiesSummary[] dailies = garminService.getTodayDailiesSummaryForUser(userId);
    return ResponseEntity.ok(dailies);
  }

  @GetMapping("/dailies")
  public ResponseEntity<DailiesSummary[]> getDailiesByDate(
      @RequestParam String userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    log.info("Fetching dailies for user: {} on date: {}", userId, date);
    DailiesSummary[] dailies = garminService.getDailiesSummaryForUser(userId, date);
    return ResponseEntity.ok(dailies);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> health = new HashMap<>();
    health.put("status", "UP");
    health.put("service", "Garmin Integration Service");
    return ResponseEntity.ok(health);
  }
}
