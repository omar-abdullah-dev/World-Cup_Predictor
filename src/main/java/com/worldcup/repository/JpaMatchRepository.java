package com.worldcup.repository;

import com.worldcup.model.Match;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA/Hibernate implementation of MatchRepository.
 */
@ApplicationScoped
@Transactional
public class JpaMatchRepository implements MatchRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public Match save(Match match) {
        if (match.getId() == null) {
            em.persist(match);
            em.flush();
        } else {
            match = em.merge(match);
        }
        return match;
    }

    @Override
    public Optional<Match> findById(Long id) {
        return Optional.ofNullable(em.find(Match.class, id));
    }

    @Override
    public List<Match> findAll() {
        return em.createQuery(
                "SELECT DISTINCT m FROM Match m "
                        + "LEFT JOIN FETCH m.homeTeamEntity "
                        + "LEFT JOIN FETCH m.awayTeamEntity "
                        + "LEFT JOIN FETCH m.round "
                        + "LEFT JOIN FETCH m.group "
                        + "ORDER BY m.kickoffDate",
                Match.class).getResultList();
    }

    @Override
    public List<Match> findByRound(Long roundId) {
        return em.createQuery(
                "SELECT DISTINCT m FROM Match m "
                        + "LEFT JOIN FETCH m.homeTeamEntity "
                        + "LEFT JOIN FETCH m.awayTeamEntity "
                        + "LEFT JOIN FETCH m.round "
                        + "LEFT JOIN FETCH m.group "
                        + "WHERE m.round.id = :roundId ORDER BY m.kickoffDate",
                Match.class)
                .setParameter("roundId", roundId)
                .getResultList();
    }

    @Override
    public List<Match> findByGroup(Long groupId) {
        return em.createQuery(
                "SELECT DISTINCT m FROM Match m "
                        + "LEFT JOIN FETCH m.homeTeamEntity "
                        + "LEFT JOIN FETCH m.awayTeamEntity "
                        + "LEFT JOIN FETCH m.round "
                        + "LEFT JOIN FETCH m.group "
                        + "WHERE m.group.id = :groupId ORDER BY m.kickoffDate",
                Match.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    @Override
    public Match update(Match match) {
        return em.merge(match);
    }

    @Override
    public void deleteById(Long id) {
        Match match = em.find(Match.class, id);
        if (match != null) em.remove(match);
    }
}
