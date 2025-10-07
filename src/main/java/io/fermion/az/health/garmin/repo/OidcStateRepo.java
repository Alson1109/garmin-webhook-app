package io.fermion.az.health.garmin.repo;

import io.fermion.az.health.garmin.entity.OidcState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface OidcStateRepo extends JpaRepository<OidcState, String> {
  @Modifying
  @Transactional
  @Query("DELETE FROM OidcState WHERE createdAt < :threshold")
  void deleteOlderThan(LocalDateTime threshold);
}
