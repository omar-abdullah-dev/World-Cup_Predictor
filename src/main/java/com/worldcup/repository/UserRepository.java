package com.worldcup.repository;

import com.worldcup.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByAdUsername(String adUsername);
    List<User> findAll();
    User update(User user);
}
