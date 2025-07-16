package com.XuebaoMaster.backend.ModuleUsage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/module-usage")
public class ModuleUsageController {

    private static final Logger logger = LoggerFactory.getLogger(ModuleUsageController.class);

    private final ModuleUsageService moduleUsageService;

    @Autowired
    public ModuleUsageController(ModuleUsageService moduleUsageService) {
        this.moduleUsageService = moduleUsageService;
        logger.info("ModuleUsageController 初始化成功");
    }

    @GetMapping("/teacher/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Long>> getTeacherTodayStats() {
        logger.info("请求获取教师今日统计");
        Map<String, Long> stats = moduleUsageService.getTeacherTodayUsageStats();
        logger.info("返回教师今日统计数据: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/teacher/week")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Long>> getTeacherWeekStats() {
        logger.info("请求获取教师本周统计");
        Map<String, Long> stats = moduleUsageService.getTeacherWeekUsageStats();
        logger.info("返回教师本周统计数据: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/student/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Long>> getStudentTodayStats() {
        logger.info("请求获取学生今日统计");
        Map<String, Long> stats = moduleUsageService.getStudentTodayUsageStats();
        logger.info("返回学生今日统计数据: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/student/week")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Long>> getStudentWeekStats() {
        logger.info("请求获取学生本周统计");
        Map<String, Long> stats = moduleUsageService.getStudentWeekUsageStats();
        logger.info("返回学生本周统计数据: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getAllStats() {
        logger.info("请求获取所有统计数据");

        // 记录当前认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.info("当前请求用户: {}, 主体类型: {}",
                    auth.getName(),
                    auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
        } else {
            logger.warn("未找到认证信息");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teacherToday", moduleUsageService.getTeacherTodayUsageStats());
        result.put("teacherWeek", moduleUsageService.getTeacherWeekUsageStats());
        result.put("studentToday", moduleUsageService.getStudentTodayUsageStats());
        result.put("studentWeek", moduleUsageService.getStudentWeekUsageStats());

        logger.info("返回所有统计数据: {}", result);
        return ResponseEntity.ok(result);
    }
}