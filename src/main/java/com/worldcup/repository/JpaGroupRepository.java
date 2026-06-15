package com.worldcup.repository;

import com.worldcup.model.Group;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class JpaGroupRepository implements GroupRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public Group save(Group group) {
        em.persist(group);
        em.flush();
        return group;
    }

    @Override
    public Optional<Group> findById(Long id) {
        List<Group> result = em.createQuery(
                "SELECT DISTINCT g FROM Group g "
                        + "LEFT JOIN FETCH g.teams "
                        + "LEFT JOIN FETCH g.round "
                        + "WHERE g.id = :id",
                Group.class)
                .setParameter("id", id)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<Group> findByName(String name) {
        return em.createQuery(
                "SELECT DISTINCT g FROM Group g "
                        + "LEFT JOIN FETCH g.teams "
                        + "LEFT JOIN FETCH g.round "
                        + "WHERE g.name = :name",
                Group.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Group> findAll() {
        return em.createQuery(
                "SELECT DISTINCT g FROM Group g "
                        + "LEFT JOIN FETCH g.teams "
                        + "LEFT JOIN FETCH g.round "
                        + "ORDER BY g.name",
                Group.class).getResultList();
    }

    @Override
    public Group update(Group group) {
        return em.merge(group);
    }

    @Override
    public boolean deleteById(Long id) {
        Group group = em.find(Group.class, id);
        if (group == null) return false;
        em.remove(group);
        return true;
    }
}
