package com.worldcup.repository;

import com.worldcup.model.Team;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class JpaTeamRepository implements TeamRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public Team save(Team team) {
        em.persist(team);
        em.flush();
        return team;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return Optional.ofNullable(em.find(Team.class, id));
    }

    @Override
    public Optional<Team> findByName(String name) {
        List<Team> results = em.createQuery(
                "SELECT t FROM Team t WHERE t.name = :name",
                Team.class)
                .setParameter("name", name)
                .getResultList();
        return results.isEmpty() ? Optional.<Team>empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Team> findAll() {
        return em.createQuery("SELECT t FROM Team t ORDER BY t.name", Team.class)
                .getResultList();
    }

    @Override
    public Team update(Team team) {
        return em.merge(team);
    }

    @Override
    public boolean deleteById(Long id) {
        Team team = em.find(Team.class, id);
        if (team == null) return false;
        em.remove(team);
        return true;
    }
}
