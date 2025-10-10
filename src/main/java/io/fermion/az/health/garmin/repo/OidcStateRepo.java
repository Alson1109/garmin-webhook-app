package io.fermion.az.health.garmin.repo;

import io.fermion.az.health.garmin.entity.OidcState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OidcStateRepository extends JpaRepository<OidcState, String> {
  // Custom queries if needed
}
