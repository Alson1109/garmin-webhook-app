package io.fermion.az.health.garmin.repo;

import io.fermion.az.health.garmin.entity.GarminUserTokens;
import io.fermion.az.health.garmin.entity.GarminUserTokensId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GarminUserTokensRepository extends JpaRepository<GarminUserTokens, GarminUserTokensId> {
  @Query(value = "SELECT u FROM GarminUserTokens u WHERE u.id.userId = ?1")
  List<GarminUserTokens> findAllByUserId(String userId);

  @Query("SELECT u FROM GarminUserTokens u WHERE u.id.userId = :userId AND u.connectStatus = 'CONNECTED'")
  GarminUserTokens findConnectedByUserId(String userId);

  @Query("SELECT u FROM GarminUserTokens u WHERE u.id.garminUserId = :garminUserId")
  GarminUserTokens findUserByGarminUserId(String garminUserId);

  @Query("SELECT COUNT(u) > 0 FROM GarminUserTokens u WHERE u.id.userId = :userId AND u.connectStatus = 'CONNECTED'")
  boolean hasConnectedAccount(String userId);
}
