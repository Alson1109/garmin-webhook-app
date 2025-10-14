package io.fermion.az.health.garmin.controller;

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

    /**
     * Optional lightweight auth: set a shared secret in env/properties to enable.
     * Example:
     *   WEBHOOK_SHARED_SECRET=superlongrandom
     * Then configure Garmin to send this header (if supported), or keep it empty to disable.
     */
    @Value("${webhook.shared.secret:}")
    private String sharedSecret;

    private boolean isAuthorized(String provided) {
        if (sharedSecret == null || sharedSecret.isBlank()) return true; // auth disabled
        return sharedSecret.equals(provided);
    }

    private void logCompact(String label, Map<String, Object> body) {
        // Log the whole body but also pull common fields up front for quick scanning in Railway
        String userId = body != null ? String.valueOf(body.getOrDefault("userId", "")) : "";
        String summaryId = body != null ? String.valueOf(body.getOrDefault("summaryId", "")) : "";
        String calendarDate = body != null ? String.valueOf(body.getOrDefault("calendarDate", "")) : "";
        Object steps = body != null ? body.get("steps") : null;

        log.info("📬 {} | userId={} summaryId={} date={} steps={}", label, userId, summaryId, calendarDate, steps);
        log.info("📦 {} FULL: {}", label, body);
    }

    // -------- DAILIES (wellness daily summary) --------
    // Garmin Portal -> Endpoint Configuration -> HEALTH – Dailies:
    // https://<your-host>/api/garmin/webhook/dailies
    @PostMapping("/dailies")
    public ResponseEntity<String> dailies(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> body) {

        if (!isAuthorized(sig)) {
            log.warn("🔒 DAILIES rejected (bad secret).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        }
        if (CollectionUtils.isEmpty(body)) {
            log.info("📬 DAILIES ping/empty payload");
            return ResponseEntity.ok("ok");
        }
        logCompact("DAILIES", body);
        return ResponseEntity.ok("ok");
    }

    // -------- ACTIVITIES (workouts) --------
    // If you enable Activities in the Garmin Portal:
    // https://<your-host>/api/garmin/webhook/activities
    @PostMapping("/activities")
    public ResponseEntity<String> activities(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> body) {

        if (!isAuthorized(sig)) {
            log.warn("🔒 ACTIVITIES rejected (bad secret).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        }
        if (CollectionUtils.isEmpty(body)) {
            log.info("📬 ACTIVITIES ping/empty payload");
            return ResponseEntity.ok("ok");
        }
        // Common activity fields: userId, activityId, startTimeInSeconds, durationInSeconds, distanceInMeters, etc.
        log.info("📬 ACTIVITIES | userId={} activityId={}",
                body != null ? body.get("userId") : null,
                body != null ? body.get("activityId") : null);
        log.info("📦 ACTIVITIES FULL: {}", body);
        return ResponseEntity.ok("ok");
    }

    // -------- REGISTRATION (optional) --------
    // Some Garmin flows send registration/change notifications here if configured
    @PostMapping("/registration")
    public ResponseEntity<String> registration(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> body) {

        if (!isAuthorized(sig)) {
            log.warn("🔒 REGISTRATION rejected (bad secret).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        }
        if (CollectionUtils.isEmpty(body)) {
            log.info("📬 REGISTRATION ping/empty payload");
            return ResponseEntity.ok("ok");
        }
        log.info("📬 REGISTRATION event: {}", body);
        return ResponseEntity.ok("ok");
    }

    // -------- Catch-all POST (some tenants send pings to base path) --------
    @PostMapping
    public ResponseEntity<String> root(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String sig,
            @RequestBody(required = false) Map<String, Object> body) {

        if (!isAuthorized(sig)) {
            log.warn("🔒 ROOT webhook rejected (bad secret).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        }
        log.info("📬 ROOT webhook: {}", body);
        return ResponseEntity.ok("ok");
    }

    // -------- Simple GET to verify reachability --------
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("✅ Garmin webhook /ping received");
        return ResponseEntity.ok(Map.of("status", "ok", "timestamp", System.currentTimeMillis()));
    }
}
