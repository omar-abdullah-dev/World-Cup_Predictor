package com.worldcup.service;

import com.worldcup.model.RoundStatus;
import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import com.worldcup.model.User;
import com.worldcup.repository.TournamentRoundRepository;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TournamentRoundService {

    private TournamentRoundRepository roundRepository;

    protected TournamentRoundService() {}

    @Inject
    public TournamentRoundService(TournamentRoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    public List<TournamentRound> getAllRounds() {
        return roundRepository.findAll();
    }

    public TournamentRound getRound(Long id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));
    }

    public TournamentRound getRoundByStage(TournamentStage stage) {
        return roundRepository.findByStage(stage).orElse(null);
    }

    public TournamentRound createRound(User adminUser, TournamentRound round) {
        SecurityService.assertAdmin(adminUser, "create round");
        if (roundRepository.findByStage(round.getStage()).isPresent()) {
            throw new IllegalArgumentException("Round for this stage already exists");
        }
        return roundRepository.save(round);
    }

    public TournamentRound openRound(User adminUser, Long id) {
        SecurityService.assertAdmin(adminUser, "open round");
        TournamentRound round = getRound(id);
        round.setStatus(RoundStatus.OPEN);
        round.setOpenedAt(LocalDateTime.now());
        return roundRepository.update(round);
    }

    public TournamentRound lockRound(User adminUser, Long id) {
        SecurityService.assertAdmin(adminUser, "lock round");
        TournamentRound round = getRound(id);
        round.setStatus(RoundStatus.LOCKED);
        round.setLockedAt(LocalDateTime.now());
        return roundRepository.update(round);
    }

    public TournamentRound closeRound(User adminUser, Long id) {
        SecurityService.assertAdmin(adminUser, "close round");
        TournamentRound round = getRound(id);
        round.setStatus(RoundStatus.CLOSED);
        round.setClosedAt(LocalDateTime.now());
        return roundRepository.update(round);
    }

    public TournamentRound updatePredictionDeadline(User adminUser, Long id, LocalDateTime deadline) {
        SecurityService.assertAdmin(adminUser, "update prediction deadline");
        TournamentRound round = getRound(id);
        round.setPredictionDeadline(deadline);
        return roundRepository.update(round);
    }
}
