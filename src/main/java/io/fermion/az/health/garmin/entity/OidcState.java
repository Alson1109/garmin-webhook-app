// src/main/java/io/fermion/az/health/garmin/entity/OidcState.java
package io.fermion.az.health.garmin.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oidc_state")
@Data
public class OidcState {
    @Id
    private String state;
    
    @Column(name = "code_verifier", nullable = false, length = 512)
    private String codeVerifier;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Default constructor
    public OidcState() {}
    
    // All-args constructor
    public OidcState(String state, String codeVerifier, String userId, LocalDateTime createdAt) {
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}
