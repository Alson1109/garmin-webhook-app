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
    
    log.info("Exchanging code for token at URL: {}", tokenUrl);
    log.info("Request body: {}", body);

    try {
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            tokenUrl, entity, TokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            TokenResponse tokenResponse = response.getBody();
            log.info("Successfully received tokens from Garmin");
            
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
                userTokenToSave.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()));
                userTokenToSave.setCreatedAt(LocalDateTime.now());
                userTokenToSave.setLastModifiedAt(LocalDateTime.now());
                userTokenToSave.setConnectStatus(GarminUserTokens.ConnectStatus.CONNECTED);
            }

            return userTokenRepo.save(userTokenToSave);
        } else {
            log.error("Token exchange failed with status: {}", response.getStatusCode());
            throw new GarminApiException("Failed to exchange code for token: HTTP " + response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("Token exchange error: {}", e.getMessage());
        throw new GarminApiException("Token exchange failed: " + e.getMessage());
    }
}
