package io.fermion.az.health.garmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oidc_state_store")
public class OidcState {
  @Id
  @Column(name = "oidc_state")
  private String oidcState;

  @Column(name = "code_verifier", nullable = false)
  private String codeVerifier;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
