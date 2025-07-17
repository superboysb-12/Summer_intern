package com.XuebaoMaster.backend.User;

import java.util.List;

public interface UserService {
    public User createUser(User user);

    public User updateUser(User user);

    public void deleteUser(Long id);

    public User getUserById(Long id);

    public User getUserByUsername(String username);

    public List<User> getAllUsers();

    public boolean checkUsernameExists(String username);

    public User login(User user);
}
