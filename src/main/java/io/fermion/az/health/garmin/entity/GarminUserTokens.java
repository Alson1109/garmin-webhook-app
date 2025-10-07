package io.fermion.az.health.garmin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "garmin_user_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "garmin_user_id" })
})
public class GarminUserTokens {

  @EmbeddedId
  private GarminUserTokensId id;

  @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
  private String accessToken;

  @Column(name = "access_token_expiry", nullable = false)
  private LocalDateTime accessTokenExpiry;

  @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
  private String refreshToken;

  @Column(name = "refresh_token_expiry", nullable = false)
  private LocalDateTime refreshTokenExpiry;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "last_modified_at")
  private LocalDateTime lastModifiedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "connect_status", nullable = false)
  private ConnectStatus connectStatus;

  public enum ConnectStatus {
    CONNECTED, DISCONNECTED
  }

  public GarminUserTokensId getId() {
    return id;
  }

  public void setId(GarminUserTokensId id) {
    this.id = id;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public LocalDateTime getAccessTokenExpiry() {
    return accessTokenExpiry;
  }

  public void setAccessTokenExpiry(LocalDateTime accessTokenExpiry) {
    this.accessTokenExpiry = accessTokenExpiry;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public LocalDateTime getRefreshTokenExpiry() {
    return refreshTokenExpiry;
  }

  public void setRefreshTokenExpiry(LocalDateTime refreshTokenExpiry) {
    this.refreshTokenExpiry = refreshTokenExpiry;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }

  public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }

  public ConnectStatus getConnectStatus() {
    return connectStatus;
  }

  public void setConnectStatus(ConnectStatus connected) {
    this.connectStatus = connected;
  }
}
