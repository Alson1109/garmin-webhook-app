package io.fermion.az.health.garmin.repo;

import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.entity.GarminUserTokensId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GarminUserTokensRepository extends JpaRepository<GarminUserTokens, GarminUserTokensId> {

  List<GarminUserTokens> findByIdUserId(String userId);

  Optional<GarminUserTokens> findByIdUserIdAndConnectStatus(String userId,
      GarminUserTokens.ConnectStatus connectStatus);

  Optional<GarminUserTokens> findByIdGarminUserId(String garminUserId);

  boolean existsByIdUserIdAndConnectStatus(String userId, GarminUserTokens.ConnectStatus connectStatus);

  @Query("SELECT u FROM GarminUserTokens u WHERE u.accessTokenExpiry < CURRENT_TIMESTAMP AND u.refreshTokenExpiry > CURRENT_TIMESTAMP")
  List<GarminUserTokens> findTokensNeedingRefresh();

  // Helper methods
  default boolean hasConnectedAccount(String userId) {
    return existsByIdUserIdAndConnectStatus(userId, GarminUserTokens.ConnectStatus.CONNECTED);
  }

  default GarminUserTokens findConnectedByUserId(String userId) {
    return findByIdUserIdAndConnectStatus(userId, GarminUserTokens.ConnectStatus.CONNECTED)
        .orElse(null);
  }
}