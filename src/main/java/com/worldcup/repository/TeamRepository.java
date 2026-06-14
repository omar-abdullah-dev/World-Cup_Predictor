package com.worldcup.repository;

import com.worldcup.model.Team;
import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    Team save(Team team);
    Optional<Team> findById(Long id);
    Optional<Team> findByName(String name);
    List<Team> findAll();
    Team update(Team team);
    boolean deleteById(Long id);
}
