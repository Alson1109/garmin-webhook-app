package io.fermion.az.health.garmin.service;

import io.fermion.az.health.garmin.dto.AuthorizationRequest;
import io.fermion.az.health.garmin.dto.DailiesSummary;
import io.fermion.az.health.garmin.dto.TokenResponse;
import io.fermion.az.health.garmin.dto.UserIdResponse;
import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.entity.GarminUserTokensId;
import io.fermion.az.health.garmin.entity.OidcState;
import io.fermion.az.health.garmin.exception.GarminApiException;
import io.fermion.az.health.garmin.repo.GarminUserTokensRepository;
import io.fermion.az.health.garmin.repo.OidcStateRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class GarminService {

    private final OidcStateRepository oidcStateRepository;
    private final GarminUserTokensRepository garminUserTokensRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GARMIN_API_BASE = "https://apis.garmin.com/wellness-api/rest";
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

    @PostConstruct
    public void init() {
        log.info("=== GARMIN CONFIGURATION ===");
        log.info("Client ID: {}", clientId);
        log.info("Redirect URI: {}", redirectUri);
        log.info("Token URL: {}", tokenUrl);
        log.info("User ID URL: {}", userIdUrl);
        log.info("Dailies URL: {}", dailiesUrl);
        log.info("=== END GARMIN CONFIG ===");
    }

    // ======================
    // AUTH & TOKEN HANDLING
    // ======================

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
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                log.info("✅ Successfully received tokens from Garmin");

                UserIdResponse userIdResponse = fetchUserId(tokenResponse.getAccessToken());
                String garminUserId = userIdResponse.getUserId();

                List<GarminUserTokens> existingTokens = garminUserTokensRepository.findByIdUserId(debiUserId);
                GarminUserTokens userTokenToSave = null;
                boolean isExisting = false;

                for (GarminUserTokens existing : existingTokens) {
                    if (existing.getId().getGarminUserId().equals(garminUserId)) {
                        // Update existing Garmin connection
                        existing.setAccessToken(tokenResponse.getAccessToken());
                        existing.setRefreshToken(tokenResponse.getRefreshToken());
                        existing.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
                        existing.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
                        existing.setLastModifiedAt(LocalDateTime.now());
                        existing.setConnectStatus(GarminUserTokens.ConnectStatus.CONNECTED);
                        userTokenToSave = existing;
                        isExisting = true;
                    } else {
                        // Disconnect any other Garmin account
                        existing.setConnectStatus(GarminUserTokens.ConnectStatus.DISCONNECTED);
                        existing.setLastModifiedAt(LocalDateTime.now());
                        garminUserTokensRepository.save(existing);
                    }
                }

                if (!isExisting) {
                    GarminUserTokensId userTokensId = new GarminUserTokensId();
                    userTokensId.setUserId(debiUserId);
                    userTokensId.setGarminUserId(garminUserId);

                    userTokenToSave = new GarminUserTokens();
                    userTokenToSave.setId(userTokensId);
                    userTokenToSave.setAccessToken(tokenResponse.getAccessToken());
                    userTokenToSave.setRefreshToken(tokenResponse.getRefreshToken());
                    userTokenToSave.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
                    userTokenToSave.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
                    userTokenToSave.setCreatedAt(LocalDateTime.now());
                    userTokenToSave.setLastModifiedAt(LocalDateTime.now());
                    userTokenToSave.setConnectStatus(GarminUserTokens.ConnectStatus.CONNECTED);
                }

                return garminUserTokensRepository.save(userTokenToSave);
            } else {
                log.error("❌ Token exchange failed with status: {}", response.getStatusCode());
                throw new GarminApiException("Failed to exchange code for token: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Token exchange error: {}", e.getMessage());
            throw new GarminApiException("Token exchange failed: " + e.getMessage());
        }
    }

    private UserIdResponse fetchUserId(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserIdResponse> response = restTemplate.exchange(userIdUrl, HttpMethod.GET, entity, UserIdResponse.class);
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

    // ======================
    // GARMIN DATA FETCH
    // ======================

    public Map<String, Object> getDailiesSummary(String userId, LocalDate date, String accessToken) {
        long startOfDay = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        String url = String.format("%s/dailies/%s?uploadStartTimeInSeconds=%d",
                GARMIN_API_BASE, userId, startOfDay);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public void logDailiesSummary(String userId, LocalDate date, String accessToken) {
        try {
            Map<String, Object> summary = getDailiesSummary(userId, date, accessToken);
            if (summary != null) {
                log.info("✅ Garmin Daily Summary for user {} on {}: {}", userId, date, summary);
            } else {
                log.warn("⚠️ No Garmin daily summary data found for user {} on {}", userId, date);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching Garmin daily summary: {}", e.getMessage(), e);
        }
    }

    public DailiesSummary[] getTodayDailiesSummaryForUser(String userId) {
    LocalDate today = LocalDate.now();
    return getDailiesSummaryForUser(userId, today);
    }

    public DailiesSummary[] getDailiesSummaryForUser(String userId, LocalDate date) {
    GarminUserTokens tokens = garminUserTokensRepository.findConnectedByUserId(userId);

    if (tokens == null) {
        throw new GarminApiException("No connected Garmin account found for user: " + userId);
    }

    // Refresh if expired
    if (tokens.getAccessTokenExpiry().isBefore(LocalDateTime.now())) {
        tokens = refreshAccessToken(tokens);
    }

    // Call the existing 3-argument version
    Map<String, Object> response = getDailiesSummary(tokens.getId().getGarminUserId(), date, tokens.getAccessToken());

    // Convert the response to DailiesSummary[] if needed (or just log/return null if not structured)
    return new DailiesSummary[] {}; // replace with parsed mapping if necessary
    }
    private GarminUserTokens refreshAccessToken(GarminUserTokens tokens) {
        if (tokens.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new GarminApiException("Refresh token expired. User needs to re-authenticate.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + auth);

        String body = String.format("grant_type=refresh_token&refresh_token=%s", tokens.getRefreshToken());
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                tokens.setAccessToken(tokenResponse.getAccessToken());
                tokens.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
                tokens.setLastModifiedAt(LocalDateTime.now());
                if (tokenResponse.getRefreshToken() != null) {
                    tokens.setRefreshToken(tokenResponse.getRefreshToken());
                    tokens.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
                }
                return garminUserTokensRepository.save(tokens);
            } else {
                throw new GarminApiException("Token refresh failed: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            throw new GarminApiException("Failed to refresh token: " + e.getMessage());
        }
    }

    // ======================
    // CONNECTION STATUS
    // ======================

    public Map<String, Object> getConnectionStatus(String userId) {
        Map<String, Object> status = new HashMap<>();
        GarminUserTokens connectedToken = garminUserTokensRepository.findConnectedByUserId(userId);

        if (connectedToken != null) {
            status.put("connected", true);
            status.put("garminUserId", connectedToken.getId().getGarminUserId());
            status.put("accessTokenExpiry", connectedToken.getAccessTokenExpiry());
            status.put("needsRefresh", connectedToken.getAccessTokenExpiry().isBefore(LocalDateTime.now()));
        } else {
            status.put("connected", false);
            status.put("message", "No Garmin account connected. Please authenticate first.");
        }

        return status;
    }

    // ======================
    // OAUTH URL BUILDER
    // ======================

    public String generateAuthorizationUrl(String userId) {
        String state = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        OidcState oidcState = new OidcState();
        oidcState.setState(state);
        oidcState.setCodeVerifier(codeVerifier);
        oidcState.setUserId(userId);
        oidcState.setCreatedAt(LocalDateTime.now());
        oidcStateRepository.save(oidcState);

        return String.format(
            "https://connect.garmin.com/oauth2/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=%s&code_challenge=%s&code_challenge_method=S256&scope=health",
            clientId, redirectUri, state, codeChallenge);
    }

    public GarminUserTokens handleOAuthCallback(String code, String state) {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setCode(code);
        request.setState(state);
        return exchangeCodeForToken(request);
    }

    // ======================
    // HELPERS
    // ======================

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
