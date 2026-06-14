package com.worldcup.repository;

import com.worldcup.model.Group;
import java.util.List;
import java.util.Optional;

public interface GroupRepository {
    Group save(Group group);
    Optional<Group> findById(Long id);
    Optional<Group> findByName(String name);
    List<Group> findAll();
    Group update(Group group);
    boolean deleteById(Long id);
}
