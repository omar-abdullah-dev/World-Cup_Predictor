package com.worldcup.repository;

import com.worldcup.model.Prediction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA/Hibernate implementation of PredictionRepository.
 */
@ApplicationScoped
@Transactional
public class JpaPredictionRepository implements PredictionRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public Prediction save(Prediction prediction) {
        if (prediction.getId() == null) {
            em.persist(prediction);
            em.flush();
        } else {
            prediction = em.merge(prediction);
        }
        return prediction;
    }

    @Override
    public Optional<Prediction> findById(Long id) {
        return Optional.ofNullable(em.find(Prediction.class, id));
    }

    @Override
    public Optional<Prediction> findByUserAndMatch(Long userId, Long matchId) {
        List<Prediction> results = em.createQuery(
                "SELECT p FROM Prediction p WHERE p.userId = :uid AND p.matchId = :mid",
                Prediction.class)
                .setParameter("uid", userId)
                .setParameter("mid", matchId)
                .getResultList();
        return results.isEmpty() ? Optional.<Prediction>empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Prediction> findByMatch(Long matchId) {
        return em.createQuery("SELECT p FROM Prediction p WHERE p.matchId = :mid", Prediction.class)
                .setParameter("mid", matchId)
                .getResultList();
    }

    @Override
    public List<Prediction> findAll() {
        return em.createQuery("SELECT p FROM Prediction p", Prediction.class).getResultList();
    }
}
