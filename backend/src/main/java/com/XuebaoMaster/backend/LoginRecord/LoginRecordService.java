package com.XuebaoMaster.backend.LoginRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
public interface LoginRecordService {
    LoginRecord createLoginRecord(LoginRecord loginRecord);
    LoginRecord createLoginRecord(Long userId);
    void deleteLoginRecord(Long id);
    LoginRecord getLoginRecordById(Long id);
    List<LoginRecord> getLoginRecordsByUserId(Long userId);
    List<LoginRecord> getLoginRecordsByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<LoginRecord> getLoginRecordsByUserIdAndTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    List<LoginRecord> getAllLoginRecords();
    Map<String, Long> getLoginStatistics(Long userId);
    Map<String, Long> getLoginStatistics(LocalDateTime startTime, LocalDateTime endTime);
} 
