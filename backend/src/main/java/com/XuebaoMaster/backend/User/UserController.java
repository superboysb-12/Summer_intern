package com.XuebaoMaster.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.XuebaoMaster.backend.util.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 
     * @return
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
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
