package io.fermion.az.health.garmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/garmin/webhook")
public class GarminWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GarminWebhookController.class);

    /**
     * Garmin will POST Dailies data to this endpoint.
     * Make sure the Garmin Developer Portal webhook URL
     * is set to:
     * https://garmin-webhook-app-production.up.railway.app/api/garmin/webhook/dailies
     */
    @PostMapping("/dailies")
    public ResponseEntity<Void> handleDailiesWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        log.info("📬 Garmin DAILIES Webhook received: {}", payload);
        return ResponseEntity.ok().build();
    }

    /**
     * Garmin may also send validation PINGs (empty POST)
     */
    @PostMapping
    public ResponseEntity<Void> handleGenericWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        log.info("📬 Garmin WEBHOOK Ping: {}", payload);
        return ResponseEntity.ok().build();
    }

    /**
     * Simple GET for testing endpoint reachability
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("✅ Garmin webhook /ping received");
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
