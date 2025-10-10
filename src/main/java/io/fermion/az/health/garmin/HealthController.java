package io.fermion.az.health.garmin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

  @GetMapping("/")
  public Map<String, String> home() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "Garmin Webhook App");
    response.put("message", "Service is running");
    return response;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "OK");
    return response;
  }
}