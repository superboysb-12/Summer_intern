package com.XuebaoMaster.backend.ModuleUsage;

import com.XuebaoMaster.backend.User.User;

import java.util.Map;

public interface ModuleUsageService {

    // Record module usage for a user
    void recordModuleUsage(User user, ModuleUsage.ModuleType moduleType);

    // Get module usage statistics for teachers for today
    Map<String, Long> getTeacherTodayUsageStats();

    // Get module usage statistics for teachers for the current week
    Map<String, Long> getTeacherWeekUsageStats();

    // Get module usage statistics for students for today
    Map<String, Long> getStudentTodayUsageStats();

    // Get module usage statistics for students for the current week
    Map<String, Long> getStudentWeekUsageStats();
}