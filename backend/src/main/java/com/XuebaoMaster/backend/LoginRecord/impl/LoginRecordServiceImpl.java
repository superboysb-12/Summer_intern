package com.XuebaoMaster.backend.LoginRecord.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.LoginRecord.LoginRecord;
import com.XuebaoMaster.backend.LoginRecord.LoginRecordRepository;
import com.XuebaoMaster.backend.LoginRecord.LoginRecordService;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class LoginRecordServiceImpl implements LoginRecordService {
    @Autowired
    private LoginRecordRepository loginRecordRepository;
    @Autowired
    private UserRepository userRepository;
    @Override
    public LoginRecord createLoginRecord(LoginRecord loginRecord) {
        if (loginRecord.getTime() == null) {
            loginRecord.setTime(LocalDateTime.now());
        }
        return loginRecordRepository.save(loginRecord);
    }
    @Override
    public LoginRecord createLoginRecord(Long userId) {
        LoginRecord loginRecord = new LoginRecord();
        loginRecord.setTime(LocalDateTime.now());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        loginRecord.setUser(user);
        return loginRecordRepository.save(loginRecord);
    }
    @Override
    public void deleteLoginRecord(Long id) {
        loginRecordRepository.deleteById(id);
    }
    @Override
    public LoginRecord getLoginRecordById(Long id) {
        return loginRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Login record not found"));
    }
    @Override
    public List<LoginRecord> getLoginRecordsByUserId(Long userId) {
        return loginRecordRepository.findByUserId(userId);
    }
    @Override
    public List<LoginRecord> getLoginRecordsByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return loginRecordRepository.findByTimeBetween(startTime, endTime);
    }
    @Override
    public List<LoginRecord> getLoginRecordsByUserIdAndTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return loginRecordRepository.findByUserIdAndTimeBetween(userId, startTime, endTime);
    }
    @Override
    public List<LoginRecord> getAllLoginRecords() {
        return loginRecordRepository.findAll();
    }
    @Override
    public Map<String, Long> getLoginStatistics(Long userId) {
        Map<String, Long> statistics = new HashMap<>();
        Long totalLogins = loginRecordRepository.countByUserId(userId);
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);
        List<LoginRecord> todayLogins = loginRecordRepository.findByUserIdAndTimeBetween(userId, startOfToday, endOfToday);
        statistics.put("totalLogins", totalLogins);
        statistics.put("todayLogins", (long) todayLogins.size());
        return statistics;
    }
    @Override
    public Map<String, Long> getLoginStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> statistics = new HashMap<>();
        Long loginCount = loginRecordRepository.countByTimeBetween(startTime, endTime);
        statistics.put("loginCount", loginCount);
        return statistics;
    }
} 
