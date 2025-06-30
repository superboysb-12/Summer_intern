package com.XuebaoMaster.backend.LoginRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/login-records")
public class LoginRecordController {
    @Autowired
    private LoginRecordService loginRecordService;

    /**
     * 创建登录记录
     * 
     * @param loginRecord 登录记录信息
     * @return 创建的登录记录
     */
    @PostMapping
    public ResponseEntity<LoginRecord> createLoginRecord(@RequestBody LoginRecord loginRecord) {
        return ResponseEntity.ok(loginRecordService.createLoginRecord(loginRecord));
    }
    
    /**
     * 根据用户ID创建登录记录
     * 
     * @param userId 用户ID
     * @return 创建的登录记录
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<LoginRecord> createLoginRecordByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(loginRecordService.createLoginRecord(userId));
    }

    /**
     * 获取所有登录记录
     * 
     * @return 登录记录列表
     */
    @GetMapping
    public ResponseEntity<List<LoginRecord>> getAllLoginRecords() {
        return ResponseEntity.ok(loginRecordService.getAllLoginRecords());
    }

    /**
     * 根据ID获取登录记录
     * 
     * @param id 登录记录ID
     * @return 登录记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoginRecord> getLoginRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(loginRecordService.getLoginRecordById(id));
    }

    /**
     * 根据用户ID获取登录记录
     * 
     * @param userId 用户ID
     * @return 登录记录列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoginRecord>> getLoginRecordsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(loginRecordService.getLoginRecordsByUserId(userId));
    }
    
    /**
     * 根据时间范围获取登录记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录记录列表
     */
    @GetMapping("/time")
    public ResponseEntity<List<LoginRecord>> getLoginRecordsByTimeBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(loginRecordService.getLoginRecordsByTimeBetween(startTime, endTime));
    }
    
    /**
     * 根据用户ID和时间范围获取登录记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录记录列表
     */
    @GetMapping("/user/{userId}/time")
    public ResponseEntity<List<LoginRecord>> getLoginRecordsByUserIdAndTimeBetween(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(loginRecordService.getLoginRecordsByUserIdAndTimeBetween(userId, startTime, endTime));
    }

    /**
     * 删除登录记录
     * 
     * @param id 登录记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoginRecord(@PathVariable Long id) {
        loginRecordService.deleteLoginRecord(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取用户登录统计信息
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<Map<String, Long>> getLoginStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(loginRecordService.getLoginStatistics(userId));
    }
    
    /**
     * 获取时间范围内的登录统计信息
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    @GetMapping("/statistics/time")
    public ResponseEntity<Map<String, Long>> getLoginStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(loginRecordService.getLoginStatistics(startTime, endTime));
    }
} 