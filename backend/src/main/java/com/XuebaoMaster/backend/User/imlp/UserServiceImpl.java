package com.XuebaoMaster.backend.User.imlp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserRepository;
import com.XuebaoMaster.backend.User.UserService;
import com.XuebaoMaster.backend.LoginRecord.LoginRecordService;
import java.util.List;
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private LoginRecordService loginRecordService;
    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    @Override
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());
        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getUserRole() != null) {
            existingUser.setUserRole(user.getUserRole());
        }
        existingUser.setSchoolClass(user.getSchoolClass());
        if (user.getStudentNumber() != null) {
            existingUser.setStudentNumber(user.getStudentNumber());
        }
        return userRepository.save(existingUser);
    }
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    @Override
    public boolean checkUsernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    @Override
    public User login(User user) {
        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        loginRecordService.createLoginRecord(existingUser.getId());
        return existingUser;
    }
}
