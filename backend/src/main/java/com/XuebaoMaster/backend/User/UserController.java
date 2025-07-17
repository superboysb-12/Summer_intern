package com.XuebaoMaster.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.XuebaoMaster.backend.util.JwtUtil;
import com.XuebaoMaster.backend.LoginRecord.LoginRecordService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 */
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginRecordService loginRecordService;

    /**
     * 
     * @param user
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    /**
     * 
     * @param user
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User authenticatedUser = userService.login(user);

        // JWT
        final String token = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                authenticatedUser.getUsername(),
                authenticatedUser.getPassword(),
                java.util.Collections.emptyList()));

        //
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", authenticatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有用户，可以通过角色参数过滤
     * 
     * @param role 用户角色，可选参数
     * @return 用户列表
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        List<User> users = userService.getAllUsers();

        // 如果指定了角色参数，进行过滤
        if (role != null && !role.isEmpty()) {
            try {
                User.UserRoleType roleType = User.UserRoleType.valueOf(role.toUpperCase());
                users = users.stream()
                        .filter(user -> user.getUserRole() == roleType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // 如果角色参数无效，忽略过滤
            }
        }

        return ResponseEntity.ok(users);
    }

    /**
     * id
     * 
     * @param id id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * id
     * 
     * @param id   id
     * @param user
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return ResponseEntity.ok(userService.updateUser(user));
    }

    /**
     * id
     * 
     * @param id id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
