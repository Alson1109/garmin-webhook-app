package io.fermion.az.health.garmin.service;

import io.fermion.az.health.garmin.dto.*;
import io.fermion.az.health.garmin.entity.*;
import io.fermion.az.health.garmin.exception.GarminApiException;
import io.fermion.az.health.garmin.repo.GarminUserTokensRepository;
import io.fermion.az.health.garmin.repo.OidcStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class GarminService {

  private final OidcStateRepository oidcStateRepository;
  private final GarminUserTokensRepository garminUserTokensRepository;
  private final RestTemplate restTemplate;
  private static final Logger log = LoggerFactory.getLogger(GarminService.class);

  @Value("${garmin.client.id}")
  private String clientId;

  @Value("${garmin.client.secret}")
  private String clientSecret;

  @Value("${garmin.redirect.uri}")
  private String redirectUri;

  @Value("${garmin.token.url}")
  private String tokenUrl;

  @Value("${garmin.user.id.url}")
  private String userIdUrl;

  @Value("${garmin.dailies.url}")
  private String dailiesUrl;

  @Transactional
  public GarminUserTokens exchangeCodeForToken(AuthorizationRequest request) {
    OidcState oidcState = oidcStateRepository.findById(request.getState())
        .orElseThrow(() -> new GarminApiException("Invalid state: " + request.getState()));

    String debiUserId = oidcState.getUserId();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
    headers.set("Authorization", "Basic " + auth);

    String body = String.format(
        "grant_type=authorization_code&code=%s&state=%s&code_verifier=%s&redirect_uri=%s",
        request.getCode(), request.getState(), oidcState.getCodeVerifier(), redirectUri);

    HttpEntity<String> entity = new HttpEntity<>(body, headers);

    log.info("Exchanging code for token at URL: {}", tokenUrl);

    try {
      ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
          tokenUrl, entity, TokenResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        TokenResponse tokenResponse = response.getBody();
        log.info("Successfully received tokens from Garmin");

        UserIdResponse userIdResponse = fetchUserId(tokenResponse.getAccessToken());
        String garminUserId = userIdResponse.getUserId();
        List<GarminUserTokens> existingTokens = garminUserTokensRepository.findByIdUserId(debiUserId);

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
            garminUserTokensRepository.save(existing);
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

        return garminUserTokensRepository.save(userTokenToSave);
      } else {
        log.error("Token exchange failed with status: {}", response.getStatusCode());
        throw new GarminApiException("Failed to exchange code for token: HTTP " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("Token exchange error: {}", e.getMessage());
      throw new GarminApiException("Token exchange failed: " + e.getMessage());
    }
  }

  private UserIdResponse fetchUserId(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<UserIdResponse> response = restTemplate.exchange(
          userIdUrl, HttpMethod.GET, entity, UserIdResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody();
      } else {
        throw new GarminApiException("Failed to fetch user ID: HTTP " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("Error fetching user ID: {}", e.getMessage());
      throw new GarminApiException("Failed to fetch user ID: " + e.getMessage());
    }
  }

  public DailiesSummary[] getDailiesSummary(String accessToken, LocalDate date) {
    try {
      String apiUrl = String.format("%s?calendarDate=%s", dailiesUrl, date.toString());

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      headers.setAccept(List.of(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);

      // FIXED: Using ParameterizedTypeReference to avoid type mismatch
      ResponseEntity<DailiesSummary[]> response = restTemplate.exchange(
          apiUrl,
          HttpMethod.GET,
          entity,
          new ParameterizedTypeReference<DailiesSummary[]>() {
          });

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody();
      } else {
        throw new GarminApiException("Failed to fetch dailies summary: HTTP " + response.getStatusCode());
      }

    } catch (Exception e) {
      log.error("Error fetching dailies summary: {}", e.getMessage());
      throw new GarminApiException("Failed to fetch dailies summary: " + e.getMessage());
    }
  }

  public DailiesSummary[] getTodayDailiesSummary(String accessToken) {
    LocalDate today = LocalDate.now();
    return getDailiesSummary(accessToken, today);
  }

  public String generateAuthorizationUrl(String userId) {
    String state = generateRandomState();
    String codeVerifier = generateCodeVerifier();

    // Store state and codeVerifier with userId for later validation
    OidcState oidcState = new OidcState();
    oidcState.setState(state);
    oidcState.setCodeVerifier(codeVerifier);
    oidcState.setUserId(userId);
    oidcState.setCreatedAt(LocalDateTime.now());
    oidcStateRepository.save(oidcState);

    String codeChallenge = generateCodeChallenge(codeVerifier);

    return String.format(
        "https://connect.garmin.com/oauth2/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=%s&code_challenge=%s&code_challenge_method=S256&scope=health",
        clientId, redirectUri, state, codeChallenge);
  }

  public GarminUserTokens handleOAuthCallback(String code, String state) {
    // Create AuthorizationRequest
    AuthorizationRequest request = new AuthorizationRequest();
    request.setCode(code);
    request.setState(state);

    // Exchange authorization code for tokens
    return exchangeCodeForToken(request);
  }

  // Helper methods
  private String generateRandomState() {
    return UUID.randomUUID().toString();
  }

  private String generateCodeVerifier() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] codeVerifier = new byte[32];
    secureRandom.nextBytes(codeVerifier);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
  }

  private String generateCodeChallenge(String codeVerifier) {
    try {
      byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(bytes, 0, bytes.length);
      byte[] digest = messageDigest.digest();
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
