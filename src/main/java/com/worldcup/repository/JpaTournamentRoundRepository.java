package com.worldcup.repository;

import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import com.worldcup.model.RoundStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class JpaTournamentRoundRepository implements TournamentRoundRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public TournamentRound save(TournamentRound round) {
        em.persist(round);
        em.flush();
        return round;
    }

    @Override
    public Optional<TournamentRound> findById(Long id) {
        return Optional.ofNullable(em.find(TournamentRound.class, id));
    }

    @Override
    public Optional<TournamentRound> findByStage(TournamentStage stage) {
        List<TournamentRound> results = em.createQuery(
                "SELECT r FROM TournamentRound r WHERE r.stage = :stage",
                TournamentRound.class)
                .setParameter("stage", stage)
                .getResultList();
        return results.isEmpty() ? Optional.<TournamentRound>empty() : Optional.of(results.get(0));
    }

    @Override
    public List<TournamentRound> findAll() {
        return em.createQuery("SELECT r FROM TournamentRound r ORDER BY r.stage", TournamentRound.class)
                .getResultList();
    }

    @Override
    public List<TournamentRound> findByStatus(RoundStatus status) {
        return em.createQuery("SELECT r FROM TournamentRound r WHERE r.status = :status ORDER BY r.stage", TournamentRound.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public TournamentRound update(TournamentRound round) {
        return em.merge(round);
    }
}
