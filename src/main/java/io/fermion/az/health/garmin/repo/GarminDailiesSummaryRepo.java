package io.fermion.az.health.garmin.repo;

import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.fermion.az.health.garmin.entity.GarminUserDailiesSummary;

@Repository
public interface GarminDailiesSummaryRepo extends JpaRepository<GarminUserDailiesSummary, Long> {

  @Query("SELECT s FROM GarminUserDailiesSummary s WHERE s.id.userId = :userId ORDER BY s.id.calendarDate DESC")
  List<GarminUserDailiesSummary> findByUserIdOrderByDateDesc(String userId);

  @Query("SELECT g FROM GarminUserDailiesSummary g WHERE g.id.userId = :userId AND g.id.calendarDate BETWEEN :startDate AND :endDate ORDER BY g.id.calendarDate DESC")
  List<GarminUserDailiesSummary> findByUserIdAndDateRange(String userId,
      LocalDate startDate,
      LocalDate endDate);
}
