package com.worldcup.repository;

import com.worldcup.model.Match;
import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    Match save(Match match);
    Optional<Match> findById(Long id);
    List<Match> findAll();
    Match update(Match match);
    void deleteById(Long id);
}
