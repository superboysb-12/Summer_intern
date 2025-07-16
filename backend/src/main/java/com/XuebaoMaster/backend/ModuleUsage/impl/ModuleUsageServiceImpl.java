package com.XuebaoMaster.backend.ModuleUsage.impl;

import com.XuebaoMaster.backend.ModuleUsage.ModuleUsage;
import com.XuebaoMaster.backend.ModuleUsage.ModuleUsageRepository;
import com.XuebaoMaster.backend.ModuleUsage.ModuleUsageService;
import com.XuebaoMaster.backend.User.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModuleUsageServiceImpl implements ModuleUsageService {

    private static final Logger logger = LoggerFactory.getLogger(ModuleUsageServiceImpl.class);

    private final ModuleUsageRepository moduleUsageRepository;

    @Autowired
    public ModuleUsageServiceImpl(ModuleUsageRepository moduleUsageRepository) {
        this.moduleUsageRepository = moduleUsageRepository;
        logger.info("ModuleUsageServiceImpl 初始化成功");
    }

    @Override
    public void recordModuleUsage(User user, ModuleUsage.ModuleType moduleType) {
        try {
            logger.info("开始记录用户 [ID:{}] [用户名:{}] [角色:{}] 的模块使用情况: {}",
                    user.getId(), user.getUsername(), user.getUserRole(), moduleType);

            ModuleUsage usage = new ModuleUsage(user, moduleType);
            logger.info("创建模块使用记录: {}", usage);

            ModuleUsage savedUsage = moduleUsageRepository.save(usage);
            logger.info("模块使用记录保存成功，ID: {}", savedUsage.getId());
        } catch (Exception e) {
            logger.error("保存模块使用记录时出错: {}", e.getMessage(), e);
            throw e; // 重新抛出异常以便上层处理
        }
    }

    @Override
    public Map<String, Long> getTeacherTodayUsageStats() {
        logger.info("获取教师今日使用统计");
        return getUsageStatsByRole(User.UserRoleType.TEACHER, getStartOfToday());
    }

    @Override
    public Map<String, Long> getTeacherWeekUsageStats() {
        logger.info("获取教师本周使用统计");
        return getUsageStatsByRole(User.UserRoleType.TEACHER, getStartOfWeek());
    }

    @Override
    public Map<String, Long> getStudentTodayUsageStats() {
        logger.info("获取学生今日使用统计");
        return getUsageStatsByRole(User.UserRoleType.STUDENT, getStartOfToday());
    }

    @Override
    public Map<String, Long> getStudentWeekUsageStats() {
        logger.info("获取学生本周使用统计");
        return getUsageStatsByRole(User.UserRoleType.STUDENT, getStartOfWeek());
    }

    private Map<String, Long> getUsageStatsByRole(User.UserRoleType role, LocalDateTime startTime) {
        logger.info("查询角色 [{}] 从 [{}] 开始的使用统计", role, startTime);

        Map<String, Long> stats = new HashMap<>();

        // Initialize with zero counts for all module types
        for (ModuleUsage.ModuleType moduleType : ModuleUsage.ModuleType.values()) {
            stats.put(moduleType.name(), 0L);
        }

        // Get actual counts
        List<Object[]> results;
        if (startTime.equals(getStartOfToday())) {
            logger.info("使用今日查询");
            results = moduleUsageRepository.countTodayUsageByRole(role, startTime);
        } else {
            logger.info("使用本周查询");
            results = moduleUsageRepository.countWeekUsageByRole(role, startTime);
        }

        logger.info("查询结果条数: {}", results.size());

        // Process the results
        for (Object[] result : results) {
            ModuleUsage.ModuleType moduleType = (ModuleUsage.ModuleType) result[0];
            Long count = (Long) result[1];
            stats.put(moduleType.name(), count);
            logger.info("模块 [{}] 使用次数: {}", moduleType, count);
        }

        return stats;
    }

    private LocalDateTime getStartOfToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        logger.info("今日开始时间: {}", startOfDay);
        return startOfDay;
    }

    private LocalDateTime getStartOfWeek() {
        LocalDate today = LocalDate.now();
        // Assuming Monday as the first day of the week
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        // If today is before Monday (weekend), get the previous Monday
        if (today.isBefore(monday)) {
            monday = monday.minusWeeks(1);
        }
        LocalDateTime startOfWeek = monday.atStartOfDay();
        logger.info("本周开始时间: {}", startOfWeek);
        return startOfWeek;
    }
}