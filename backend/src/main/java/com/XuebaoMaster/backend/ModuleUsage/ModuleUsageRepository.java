package com.XuebaoMaster.backend.ModuleUsage;

import com.XuebaoMaster.backend.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ModuleUsageRepository extends JpaRepository<ModuleUsage, Long> {

    // Count usages by module type and role for today
    @Query("SELECT m.moduleType, COUNT(m) FROM ModuleUsage m WHERE m.userRole = :userRole AND m.accessTime >= :startOfDay GROUP BY m.moduleType")
    List<Object[]> countTodayUsageByRole(@Param("userRole") User.UserRoleType userRole,
            @Param("startOfDay") LocalDateTime startOfDay);

    // Count usages by module type and role for the current week
    @Query("SELECT m.moduleType, COUNT(m) FROM ModuleUsage m WHERE m.userRole = :userRole AND m.accessTime >= :startOfWeek GROUP BY m.moduleType")
    List<Object[]> countWeekUsageByRole(@Param("userRole") User.UserRoleType userRole,
            @Param("startOfWeek") LocalDateTime startOfWeek);

    // Find all usages by user for a time range
    List<ModuleUsage> findByUserAndAccessTimeBetween(User user, LocalDateTime start, LocalDateTime end);
}