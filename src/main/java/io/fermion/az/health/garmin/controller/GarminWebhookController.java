package io.fermion.az.health.garmin.controller;

import io.fermion.az.health.garmin.webhook.WebhookCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/garmin/webhook")
public class GarminWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GarminWebhookController.class);

    private final WebhookCache cache;

    public GarminWebhookController(WebhookCache cache) {
        this.cache = cache;
    }

    // Optional shared secret (leave empty to disable)
    @Value("${webhook.shared.secret:}")
    private String sharedSecret;

    private boolean isAuthorized(String provided) {
        if (sharedSecret == null || sharedSecret.isBlank()) return true;
        return sharedSecret.equals(provided);
    }

    private void logCompact(String label, Map<String, Object> body) {
        String userId = body != null ? String.valueOf(body.getOrDefault("userId", "")) : "";
        String summaryId = body != null ? String.valueOf(body.getOrDefault("summaryId", "")) : "";
        String calendarDate = body != null ? String.valueOf(body.getOrDefault("calendarDate", "")) : "";
        Object steps = body != null ? body.get("steps") : null;

        log.info("📬 {} | userId={} summaryId={} date={} steps={}", label, userId, summaryId, calendarDate, steps);
        log.info("📦 {} FULL: {}", label, body);
    }

    /** Dailies push from Garmin (configure this URL in the Garmin portal) */
    @PostMapping("/dailies")
public ResponseEntity<Void> handleDailiesWebhook(
    @RequestBody List<Map<String, Object>> payload) {

  log.info("📬 DAILIES received {} item(s): {}", payload != null ? payload.size() : 0, payload);
  return ResponseEntity.ok().build();
}
    
    /** Optional: activities webhook (enable in portal if needed) */
    @PostMapping("/activities")
    public ResponseEntity<String> handleActivities(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> payload) {

        if (!isAuthorized(sig)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        if (CollectionUtils.isEmpty(payload)) {
            log.info("📬 ACTIVITIES ping/empty payload");
            return ResponseEntity.ok("ok");
        }
        log.info("📬 ACTIVITIES | userId={} activityId={}", payload.get("userId"), payload.get("activityId"));
        log.info("📦 ACTIVITIES FULL: {}", payload);
        return ResponseEntity.ok("ok");
    }

    /** Optional: registration webhook */
    @PostMapping("/registration")
    public ResponseEntity<String> handleRegistration(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> payload) {

        if (!isAuthorized(sig)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        if (CollectionUtils.isEmpty(payload)) {
            log.info("📬 REGISTRATION ping/empty payload");
            return ResponseEntity.ok("ok");
        }
        log.info("📬 REGISTRATION event: {}", payload);
        return ResponseEntity.ok("ok");
    }

    /** Catch-all POST (some tenants send pings to the base path) */
    @PostMapping
    public ResponseEntity<String> handleRoot(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> payload) {

        if (!isAuthorized(sig)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        log.info("📬 ROOT webhook: {}", payload);
        return ResponseEntity.ok("ok");
    }

    /** Quick reachability check in browser */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("✅ Garmin webhook /ping received");
        return ResponseEntity.ok(Map.of("status", "ok", "timestamp", System.currentTimeMillis()));
    }

    /** NEW: let the app read the latest pushed dailies (no DB required) */
    @GetMapping("/last")
    public ResponseEntity<Map<String, Object>> last(@RequestParam String garminUserId) {
        Map<String, Object> payload = cache.get(garminUserId);
        if (payload == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No data yet"));
        }
        Map<String, Object> summary = Map.of(
                "calendarDate", payload.get("calendarDate"),
                "steps", payload.get("steps"),
                "distanceInMeters", payload.get("distanceInMeters"),
                "activeKilocalories", payload.get("activeKilocalories"),
                "bmrKilocalories", payload.get("bmrKilocalories"),
                "avgHr", payload.get("averageHeartRateInBeatsPerMinute"),
                "restHr", payload.get("restingHeartRateInBeatsPerMinute")
        );
        return ResponseEntity.ok(Map.of("success", true, "data", summary, "raw", payload));
    }
}
