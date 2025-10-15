package io.fermion.az.health.garmin.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/garmin/webhook")
public class GarminWebhookController {
  private static final Logger log = LoggerFactory.getLogger(GarminWebhookController.class);

  @PostMapping(path="/dailies", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String,Object>> receiveDailies(@RequestBody List<Map<String,Object>> payload){
    log.info("📬 DAILIES received {} record(s)", payload.size());
    if (!payload.isEmpty()) log.info("First record: {}", payload.get(0));
    return ResponseEntity.ok(Map.of("status","ok","received", payload.size()));
  }

  @GetMapping("/ping")
  public Map<String,Object> ping() {
    return Map.of("status","ok","timestamp", System.currentTimeMillis());
  }
}
