package io.fermion.az.health.garmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "io.fermion.az.health.garmin.repo")
public class GarminDataFetchApplication {

  public static void main(String[] args) {
    SpringApplication.run(GarminDataFetchApplication.class, args);
  }
}