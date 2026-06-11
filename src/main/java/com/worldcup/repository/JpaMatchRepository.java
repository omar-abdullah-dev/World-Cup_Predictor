package com.worldcup.repository;

import com.worldcup.model.Match;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA/Hibernate implementation of MatchRepository.
 * Marked @Alternative; activate in beans.xml if PostgreSQL is configured.
 */
@Alternative
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
        return em.createQuery("SELECT m FROM Match m ORDER BY m.kickoffDate", Match.class).getResultList();
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
