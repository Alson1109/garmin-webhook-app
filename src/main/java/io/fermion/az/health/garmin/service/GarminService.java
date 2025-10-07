package io.fermion.az.health.garmin.service;

import io.fermion.az.health.garmin.dto.*;
import io.fermion.az.health.garmin.entity.GarminDailiesSummaryId;
import io.fermion.az.health.garmin.entity.GarminUserDailiesSummary;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.entity.GarminUserTokensId;
import io.fermion.az.health.garmin.entity.OidcState;
import io.fermion.az.health.garmin.exception.GarminApiException;
import io.fermion.az.health.garmin.repo.UserTokenRepo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import io.fermion.az.health.garmin.repo.GarminDailiesSummaryRepo;
// import io.fermion.az.health.garmin.repo.InMemoryUserTokenRepo;
import io.fermion.az.health.garmin.repo.OidcStateRepo;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class GarminService {
  private final UserTokenRepo userTokenRepo;
  private final OidcStateRepo oidcStateRepo;
  private final GarminDailiesSummaryRepo garminDailiesSummaryRepo;
  private final RestTemplate restTemplate;

  @Value("${garmin.clientId}")
  private String clientId;
  @Value("${garmin.clientSecret}")
  private String clientSecret;
  @Value("${garmin.baseUrl}")
  private String baseUrl;
  @Value("${garmin.redirectUri}")
  private String redirectUri;
  @Value("${garmin.authorizationUrl}")
  private String authorizationUrl;
  @Value("${garmin.tokenUrl}")
  private String tokenUrl;

  @PostConstruct // Add this method
  public void logConfiguration() {
    log.info("=== Garmin Configuration ===");
    log.info("clientId: " + clientId);
    log.info("baseUrl: " + baseUrl);
    log.info("tokenUrl: " + tokenUrl);
    log.info("authorizationUrl: " + authorizationUrl);
    log.info("redirectUri: " + redirectUri);
    log.info("============================");
  }

  // public GarminService(UserTokenRepo userTokenRepository, RestTemplate
  // restTemplate) {
  // this.userTokenRepository = userTokenRepository;
  // this.restTemplate = restTemplate;
  // }
  public GarminService(UserTokenRepo userTokenRepo, OidcStateRepo oidcStateRepo,
      GarminDailiesSummaryRepo garminDailiesSummaryRepo, RestTemplate restTemplate) {
    this.userTokenRepo = userTokenRepo;
    this.oidcStateRepo = oidcStateRepo;
    this.garminDailiesSummaryRepo = garminDailiesSummaryRepo;
    this.restTemplate = restTemplate;
  }

  @Transactional
  public String generateAuthorizationUrl(String debiUserId) {
    String state = UUID.randomUUID().toString();
    String codeVerifier = generateCodeVerifier();
    String codeChallenge = generateCodeChallenge(codeVerifier);

    OidcState oidcState = new OidcState();
    oidcState.setOidcState(state);
    oidcState.setCodeVerifier(codeVerifier);
    oidcState.setUserId(debiUserId);
    oidcState.setCreatedAt(LocalDateTime.now());
    oidcStateRepo.save(oidcState);

    // Clean up old OidcState entries older than 1 hour
    oidcStateRepo.deleteOlderThan(LocalDateTime.now().minusSeconds(3600));

    return String.format(
        "%s?client_id=%s&response_type=code&state=%s&redirect_uri=%s&code_challenge=%s&code_challenge_method=S256",
        authorizationUrl, clientId, state, redirectUri, codeChallenge);
  }

  @Transactional
  public GarminUserTokens exchangeCodeForToken(AuthorizationRequest request) {
    OidcState oidcState = oidcStateRepo.findById(request.getState())
        .orElseThrow(() -> new GarminApiException("Invalid state: " + request.getState()));

    String debiUserId = oidcState.getUserId();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String auth = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
    headers.set("Authorization", "Basic " + auth);

    String body = String.format(
        "grant_type=authorization_code&code=%s&state=%s&code_verifier=%s&redirect_uri=%s",
        request.getCode(), request.getState(), oidcState.getCodeVerifier(), redirectUri);

    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
        tokenUrl, entity, TokenResponse.class);

    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      TokenResponse tokenResponse = response.getBody();
      UserIdResponse userIdResponse = fetchUserId(tokenResponse.getAccessToken());
      String garminUserId = userIdResponse.getUserId();
      List<GarminUserTokens> existingTokens = userTokenRepo.findAllByUserId(debiUserId);

      GarminUserTokens userTokenToSave = null;
      boolean isExisting = false;

      for (GarminUserTokens existing : existingTokens) {
        if (existing.getId().getGarminUserId().equals(garminUserId)) {
          // Same Garmin account: update and connect
          existing.setAccessToken(tokenResponse.getAccessToken());
          existing.setRefreshToken(tokenResponse.getRefreshToken());
          existing.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
          existing.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
          existing.setLastModifiedAt(LocalDateTime.now());
          existing.setConnectStatus(GarminUserTokens.ConnectStatus.CONNECTED);
          userTokenToSave = existing;
          isExisting = true;
        } else {
          // Different Garmin account: disconnect
          existing.setConnectStatus(GarminUserTokens.ConnectStatus.DISCONNECTED);
          existing.setLastModifiedAt(LocalDateTime.now());
          userTokenRepo.save(existing);
        }
      }

      if (!isExisting) {
        // New Garmin account: create and connect
        GarminUserTokensId userTokensId = new GarminUserTokensId();
        userTokensId.setUserId(debiUserId);
        userTokensId.setGarminUserId(garminUserId);

        userTokenToSave = new GarminUserTokens();
        userTokenToSave.setId(userTokensId);
        userTokenToSave.setAccessToken(tokenResponse.getAccessToken());
        userTokenToSave.setRefreshToken(tokenResponse.getRefreshToken());
        userTokenToSave.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
        userTokenToSave
            .setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
        userTokenToSave.setCreatedAt(LocalDateTime.now());
        userTokenToSave.setLastModifiedAt(LocalDateTime.now());
        userTokenToSave.setConnectStatus(GarminUserTokens.ConnectStatus.CONNECTED);
      }

      return userTokenRepo.save(userTokenToSave);
    } else {
      throw new GarminApiException("Failed to exchange code for token");
    }
  }

  public UserIdResponse fetchUserId(String accessToken) {
    String url = String.format("%s/user/id", baseUrl);
    log.info("Making request to URL: " + url); // Add this line
    log.info("Base URL configured: " + baseUrl); // Add this line

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<UserIdResponse> response = restTemplate.exchange(
        String.format("%s/user/id", baseUrl), HttpMethod.GET, entity, UserIdResponse.class);
    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      return response.getBody();
    } else {
      throw new GarminApiException("Failed to fetch user ID");
    }
  }

  public List<String> fetchUserPermissions(String userId) {
    GarminUserTokens userToken = userTokenRepo.findConnectedByUserId(userId);
    if (userToken == null) {
      throw new GarminApiException("No connected Garmin account found for user: " + userId);
    }

    if (userToken.getAccessTokenExpiry().isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))) {
      userToken = refreshAccessToken(userToken);
    }

    String accessToken = userToken.getAccessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    String url = String.format("%s/user/permissions", baseUrl);
    log.info("Fetching user permissions from URL: {}", url);

    try {
      ResponseEntity<List<String>> response = restTemplate.exchange(
          url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<String>>() {
          });

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.info("Fetched user permissions successfully for userId: {}", userId);
        return response.getBody();
      } else {
        log.warn("Unexpected response status for permissions: {}", response.getStatusCode());
        throw new GarminApiException("Failed to fetch user permissions: HTTP " + response.getStatusCode());
      }
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        log.warn("Permissions endpoint returned 401 Unauthorized for userId: {}. This may indicate insufficient scope.",
            userId);
        throw new GarminApiException("Insufficient permissions or invalid scope for user permissions endpoint");
      } else {
        log.error("HTTP error fetching permissions for userId {}: {}", userId, e.getMessage());
        throw new GarminApiException("Failed to fetch user permissions: " + e.getMessage());
      }
    } catch (Exception e) {
      log.error("Unexpected error fetching permissions for userId {}: {}", userId, e.getMessage());
      throw new GarminApiException("Failed to fetch user permissions: " + e.getMessage());
    }
  }

  public DailiesSummary[] fetchDailiesData(String userId, Long uploadStartTimeInSeconds,
      Long uploadEndTimeInSeconds) {
    GarminUserTokens userToken = userTokenRepo.findConnectedByUserId(userId);
    if (userToken == null) {
      throw new GarminApiException("No connected Garmin account found for user: " + userId);
    }

    String garminUserId = userToken.getId().getGarminUserId();
    log.info("Fetched user token: {}", garminUserId);
    if (userToken.getAccessTokenExpiry().isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))) {
      userToken = refreshAccessToken(userToken);
    }

    String accessToken = userToken.getAccessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    log.info(headers.toString());
    HttpEntity<String> entity = new HttpEntity<>(headers);

    // Validate and set default date range if not provided
    if (uploadStartTimeInSeconds == null || uploadEndTimeInSeconds == null) {
      // Default to recent past data (yesterday) since we don't have future data
      long now = System.currentTimeMillis() / 1000;
      long yesterdayStart = now - 86400 - (now % 86400); // Start of yesterday in UTC
      uploadStartTimeInSeconds = yesterdayStart;
      uploadEndTimeInSeconds = yesterdayStart + 86399; // End of yesterday (23:59:59)
      log.info("Using default date range - Start: {}, End: {}", uploadStartTimeInSeconds, uploadEndTimeInSeconds);
    } else {
      // Validate the provided time range
      long range = uploadEndTimeInSeconds - uploadStartTimeInSeconds;
      if (range > 86399) {
        throw new GarminApiException("Time range exceeds the maximum of 86400 seconds (24 hours)");
      }
      if (range < 0) {
        throw new GarminApiException("End time must be after start time");
      }
      log.info("Using provided date range - Start: {}, End: {}", uploadStartTimeInSeconds, uploadEndTimeInSeconds);
    }

    // Use the correct Garmin Health API endpoint format
    String url = String.format(
        "%s/dailies?uploadStartTimeInSeconds=%d&uploadEndTimeInSeconds=%d",
        baseUrl, uploadStartTimeInSeconds, uploadEndTimeInSeconds);
    log.info("Fetching dailies data from URL: {}", url);

    try {
      ResponseEntity<DailiesSummary[]> response = restTemplate.exchange(
          url, HttpMethod.GET, entity, DailiesSummary[].class);
      log.info("Response status: {}, body length: {}", response.getStatusCode(),
          response.getBody() != null ? response.getBody().length : 0);

      if (response.getStatusCode() == HttpStatus.OK) {
        DailiesSummary[] body = response.getBody();
        if (body != null && body.length > 0) {
          log.info("Successfully fetched {} dailies summaries for userId: {}", body.length, userId);
          return body;
        } else {
          log.warn("No dailies data found for userId: {} in the requested time range", userId);
          // Return empty array instead of throwing exception
          return new DailiesSummary[0];
        }
      } else {
        log.warn("Unexpected response for dailies data: status={}, body={}", response.getStatusCode(),
            response.getBody());
        throw new GarminApiException("Failed to fetch dailies data: HTTP " + response.getStatusCode());
      }
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      String errorMessage = e.getMessage();

      // Handle specific Garmin API errors
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST && errorMessage.contains("InvalidPullTokenException")) {
        log.warn(
            "No data available for userId {} in the requested time range. This is normal for new devices or recent connections.",
            userId);
        // Return empty array instead of throwing exception for new devices
        return new DailiesSummary[0];
      }

      log.error("HTTP client error fetching dailies for userId {}: status={}, message={}", userId, e.getStatusCode(),
          errorMessage);
      throw new GarminApiException("Failed to fetch dailies data: " + e.getStatusCode() + " - " + errorMessage);
    } catch (Exception e) {
      log.error("Unexpected error fetching dailies for userId {}: {}", userId, e.getMessage());
      throw new GarminApiException("Failed to fetch dailies data: " + e.getMessage());
    }
  }

  public void deregisterUser(String userId) {
    GarminUserTokensId userTokensId = new GarminUserTokensId();
    userTokensId.setUserId(userId);
    GarminUserTokens userToken = userTokenRepo.findById(userTokensId)
        .orElseThrow(() -> new GarminApiException("User not found"));
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + userToken.getAccessToken());
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<Void> response = restTemplate.exchange(
        String.format("%s/user/registration", baseUrl),
        HttpMethod.DELETE, entity, Void.class);
    if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
      userTokenRepo.deleteById(userTokensId);
    } else {
      throw new GarminApiException("Failed to deregister user");
    }
  }

  private GarminUserTokens refreshAccessToken(GarminUserTokens userToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String auth = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
    headers.set("Authorization", "Basic " + auth);

    String body = String.format(
        "grant_type=refresh_token&refresh_token=%s&client_id=%s&client_secret=%s",
        userToken.getRefreshToken(), clientId, clientSecret);

    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
        tokenUrl, entity, TokenResponse.class);

    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      TokenResponse tokenResponse = response.getBody();
      userToken.setAccessToken(tokenResponse.getAccessToken());
      userToken.setRefreshToken(tokenResponse.getRefreshToken());
      userToken.setAccessTokenExpiry(
          LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
      userToken.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
      return userTokenRepo.save(userToken);
    } else {
      throw new GarminApiException("Failed to refresh token");
    }
  }

  private String generateCodeVerifier() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] codeVerifier = new byte[32];
    secureRandom.nextBytes(codeVerifier);
    return Base64.encodeBase64URLSafeString(codeVerifier);
  }

  private String generateCodeChallenge(String codeVerifier) {
    byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
    byte[] digest = DigestUtils.sha256(bytes);
    return Base64.encodeBase64URLSafeString(digest);
  }

  public void processDailiesPush(String pushData) {
    try {
      log.info("Processing dailies push data: {}", pushData);

      // Handle empty or null data
      if (pushData == null || pushData.trim().isEmpty()) {
        log.warn("Received empty push data");
        return;
      }

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(pushData);

      // Check if it's a test payload or invalid format
      if (!root.isArray()) {
        log.info("Received non-array JSON (possibly test data): {}", pushData);
        return;
      }

      DailiesSummary[] summaries = mapper.treeToValue(root, DailiesSummary[].class);
      if (summaries == null || summaries.length == 0) {
        log.info("No dailies summary found in push data");
        return;
      }

      log.info("Processing {} dailies summaries", summaries.length);

      for (DailiesSummary summary : summaries) {
        try {
          if (summary.getUserId() == null) {
            log.warn("Skipping summary with null userId");
            continue;
          }

          GarminUserTokens userToken = userTokenRepo.findUserByGarminUserId(summary.getUserId());
          if (userToken == null) {
            log.warn("User token not found for Garmin ID: {}", summary.getUserId());
            continue;
          }

          GarminUserDailiesSummary entity = convertToEntity(summary, userToken);
          garminDailiesSummaryRepo.save(entity);

          log.info("Successfully saved dailies summary for user: {}, date: {}, steps: {}",
              userToken.getId().getUserId(), summary.getCalendarDate(), summary.getSteps());

        } catch (Exception e) {
          log.error("Failed to process individual summary for Garmin ID {}: {}",
              summary.getUserId(), e.getMessage());
        }
      }

      log.info("Completed processing dailies push");

    } catch (Exception e) {
      log.error("Error processing dailies push: {}", e.getMessage(), e);
      // Don't throw exception - we want to return 200 OK to Garmin
      log.warn("Continuing despite error to avoid webhook retry storms");
    }
  }

  private GarminUserDailiesSummary convertToEntity(DailiesSummary summary, GarminUserTokens userToken) {
    GarminUserDailiesSummary entity = new GarminUserDailiesSummary();

    GarminDailiesSummaryId compositeId = new GarminDailiesSummaryId(
        userToken.getId().getUserId(),
        userToken.getId().getGarminUserId(),
        LocalDate.parse(summary.getCalendarDate()));

    // Basic identification
    entity.setId(compositeId);
    entity.setSummaryId(summary.getSummaryId());
    entity.setActivityType(summary.getActivityType());

    // Calorie and Activity Metrics
    entity.setActiveKilocalories(summary.getActiveKilocalories());
    entity.setBmrKilocalories(summary.getBmrKilocalories());
    entity.setSteps(summary.getSteps());
    entity.setPushes(summary.getPushes());
    entity.setDistanceInMeters(summary.getDistanceInMeters());
    entity.setPushDistanceInMeters(summary.getPushDistanceInMeters());

    // Time Metrics (in seconds)
    entity.setDurationInSeconds(summary.getDurationInSeconds());
    entity.setActiveTimeInSeconds(summary.getActiveTimeInSeconds());
    entity.setStartTimeInSeconds(summary.getStartTimeInSeconds());
    entity.setStartTimeOffsetInSeconds(summary.getStartTimeOffsetInSeconds());
    entity.setModerateIntensityDurationInSeconds(summary.getModerateIntensityDurationInSeconds());
    entity.setVigorousIntensityDurationInSeconds(summary.getVigorousIntensityDurationInSeconds());

    // Physical Activity
    entity.setFloorsClimbed(summary.getFloorsClimbed());

    // Heart Rate Metrics
    entity.setMinHeartRateInBeatsPerMinute(summary.getMinHeartRateInBeatsPerMinute());
    entity.setMaxHeartRateInBeatsPerMinute(summary.getMaxHeartRateInBeatsPerMinute());
    entity.setAverageHeartRateInBeatsPerMinute(summary.getAverageHeartRateInBeatsPerMinute());
    entity.setRestingHeartRateInBeatsPerMinute(summary.getRestingHeartRateInBeatsPerMinute());
    entity.setTimeOffsetHeartRateSamples(summary.getTimeOffsetHeartRateSamples());
    entity.setSource(summary.getSource());

    // Goals
    entity.setStepsGoal(summary.getStepsGoal());
    entity.setPushesGoal(summary.getPushesGoal());
    entity.setIntensityDurationGoalInSeconds(summary.getIntensityDurationGoalInSeconds());
    entity.setFloorsClimbedGoal(summary.getFloorsClimbedGoal());

    // Stress Metrics
    entity.setAverageStressLevel(summary.getAverageStressLevel());
    entity.setMaxStressLevel(summary.getMaxStressLevel());
    entity.setStressDurationInSeconds(summary.getStressDurationInSeconds());
    entity.setRestStressDurationInSeconds(summary.getRestStressDurationInSeconds());
    entity.setActivityStressDurationInSeconds(summary.getActivityStressDurationInSeconds());
    entity.setLowStressDurationInSeconds(summary.getLowStressDurationInSeconds());
    entity.setMediumStressDurationInSeconds(summary.getMediumStressDurationInSeconds());
    entity.setHighStressDurationInSeconds(summary.getHighStressDurationInSeconds());
    entity.setStressQualifier(summary.getStressQualifier());

    // Body Battery
    entity.setBodyBatteryChargedValue(summary.getBodyBatteryChargedValue());
    entity.setBodyBatteryDrainedValue(summary.getBodyBatteryDrainedValue());

    return entity;
  }

  public void handleDeregistration(String garminUserId) {
    GarminUserTokens userToken = userTokenRepo.findUserByGarminUserId(garminUserId);
    if (userToken != null) {
      userToken.setConnectStatus(GarminUserTokens.ConnectStatus.DISCONNECTED);
      userToken.setLastModifiedAt(LocalDateTime.now());
      userTokenRepo.save(userToken);
      log.info("Marked user as disconnected for Garmin ID: {}", garminUserId);
    } else {
      log.warn("No user token found for Garmin ID: {}", garminUserId);
    }
  }

  public boolean hasConnectedAccount(String userId) {
    return userTokenRepo.hasConnectedAccount(userId);
  }

  public boolean checkStateExists(String state) {
    return oidcStateRepo.existsById(state);
  }

  public Map<String, Object> testConnection(String userId) {
    try {
      // Step 1: Find connected user token
      GarminUserTokens userToken = userTokenRepo.findConnectedByUserId(userId);
      if (userToken == null) {
        return Map.of(
            "step", "2",
            "error", "No connected user token found",
            "userId", userId);
      }

      String garminUserId = userToken.getId().getGarminUserId();
      String accessToken = userToken.getAccessToken();

      // Step 2: Check token expiry
      boolean isExpired = userToken.getAccessTokenExpiry().isBefore(LocalDateTime.now());

      // Step 3: Test API call to Garmin
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      HttpEntity<String> entity = new HttpEntity<>(headers);

      String url = baseUrl + "/user/id";
      log.info("Testing Garmin API call to: {}", url);

      try {
        ResponseEntity<UserIdResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, UserIdResponse.class);

        return Map.of(
            "step", "3",
            "success", true,
            "userId", userId,
            "garminUserId", garminUserId,
            "tokenExpired", isExpired,
            "apiResponse", response.getStatusCode().toString(),
            "message", "Garmin API connection test successful");

      } catch (Exception apiError) {
        return Map.of(
            "step", "3",
            "error", "Garmin API call failed: " + apiError.getMessage(),
            "userId", userId,
            "garminUserId", garminUserId,
            "tokenExpired", isExpired);
      }

    } catch (Exception e) {
      return Map.of(
          "step", "unknown",
          "error", "Test connection failed: " + e.getMessage(),
          "userId", userId);
    }
  }

  public Map<String, Object> registerWebhook(String userId) {
    try {
      GarminUserTokens userToken = userTokenRepo.findConnectedByUserId(userId);
      if (userToken == null) {
        return Map.of(
            "error", "No connected Garmin account found for user: " + userId,
            "userId", userId);
      }

      String accessToken = userToken.getAccessToken();
      String webhookUrl = "https://app.debiwellness.com/api/garmin/dailies"; // Your webhook URL

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Webhook registration payload
      Map<String, Object> webhookPayload = Map.of(
          "webhookURL", webhookUrl,
          "webhookToken", "your-webhook-token", // You should generate a secure token
          "eventTypes", java.util.List.of("DAILY_SUMMARIES", "ACTIVITIES"));

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(webhookPayload, headers);
      String url = baseUrl + "/webhook";

      log.info("Registering webhook at URL: {} with payload: {}", url, webhookPayload);

      try {
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, Map.class);

        log.info("Webhook registration response: status={}, body={}",
            response.getStatusCode(), response.getBody());

        return Map.of(
            "success", true,
            "message", "Webhook registered successfully",
            "webhookUrl", webhookUrl,
            "userId", userId,
            "garminUserId", userToken.getId().getGarminUserId(),
            "response", response.getBody() != null ? response.getBody() : Map.of());

      } catch (org.springframework.web.client.HttpClientErrorException e) {
        log.error("HTTP error registering webhook for userId {}: status={}, message={}",
            userId, e.getStatusCode(), e.getMessage());
        return Map.of(
            "error", "Failed to register webhook: " + e.getStatusCode() + " - " + e.getMessage(),
            "userId", userId,
            "webhookUrl", webhookUrl);
      }

    } catch (Exception e) {
      log.error("Unexpected error registering webhook for userId {}: {}", userId, e.getMessage());
      return Map.of(
          "error", "Failed to register webhook: " + e.getMessage(),
          "userId", userId);
    }
  }

  public Map<String, Object> getRecentWebhookData(String userId) {
    try {
      // Query recent data from database
      List<GarminUserDailiesSummary> recentData = garminDailiesSummaryRepo
          .findByUserIdOrderByDateDesc(userId);

      if (recentData.isEmpty()) {
        return Map.of(
            "message", "No webhook data received yet",
            "userId", userId,
            "totalRecords", 0,
            "data", java.util.Collections.emptyList());
      }

      // Convert to response format
      java.util.List<Map<String, Object>> stepData = new java.util.ArrayList<>();
      int totalSteps = 0;

      for (GarminUserDailiesSummary summary : recentData.subList(0, Math.min(7, recentData.size()))) {
        Map<String, Object> dayData = Map.of(
            "date", summary.getId().getCalendarDate().toString(),
            "steps", summary.getSteps() != null ? summary.getSteps() : 0,
            "calories", summary.getActiveKilocalories() != null ? summary.getActiveKilocalories() : 0,
            "distance", summary.getDistanceInMeters() != null ? summary.getDistanceInMeters() : 0,
            "heartRate",
            summary.getAverageHeartRateInBeatsPerMinute() != null ? summary.getAverageHeartRateInBeatsPerMinute() : 0);
        stepData.add(dayData);
        totalSteps += summary.getSteps() != null ? summary.getSteps() : 0;
      }

      return Map.of(
          "message", "Webhook data found",
          "userId", userId,
          "totalRecords", recentData.size(),
          "totalSteps", totalSteps,
          "data", stepData);

    } catch (Exception e) {
      log.error("Error retrieving webhook data for userId {}: {}", userId, e.getMessage());
      return Map.of(
          "error", "Failed to retrieve webhook data: " + e.getMessage(),
          "userId", userId);
    }
  }

}
