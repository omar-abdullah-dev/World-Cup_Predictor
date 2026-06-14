package com.worldcup.repository;

import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import com.worldcup.model.RoundStatus;
import java.util.List;
import java.util.Optional;

public interface TournamentRoundRepository {
    TournamentRound save(TournamentRound round);
    Optional<TournamentRound> findById(Long id);
    Optional<TournamentRound> findByStage(TournamentStage stage);
    List<TournamentRound> findAll();
    List<TournamentRound> findByStatus(RoundStatus status);
    TournamentRound update(TournamentRound round);
}
