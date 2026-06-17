package com.worldcup.repository;

import com.worldcup.model.WhitelistEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class JpaWhitelistRepository implements WhitelistRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public WhitelistEntry save(WhitelistEntry entry) {
        em.persist(entry);
        em.flush();
        return entry;
    }

    @Override
    public Optional<WhitelistEntry> findById(Long id) {
        return Optional.ofNullable(em.find(WhitelistEntry.class, id));
    }

    @Override
    public Optional<WhitelistEntry> findByAdUsername(String adUsername) {
        List<WhitelistEntry> results = em.createQuery(
                "SELECT w FROM WhitelistEntry w WHERE w.adUsername = :username",
                WhitelistEntry.class)
                .setParameter("username", adUsername)
                .getResultList();
        return results.isEmpty() ? Optional.<WhitelistEntry>empty() : Optional.of(results.get(0));
    }

    @Override
    public List<WhitelistEntry> findAll() {
        return em.createQuery("SELECT w FROM WhitelistEntry w ORDER BY w.addedAt DESC", WhitelistEntry.class)
                .getResultList();
    }

    @Override
    public WhitelistEntry update(WhitelistEntry entry) {
        return em.merge(entry);
    }

    @Override
    public boolean deleteById(Long id) {
        WhitelistEntry entry = em.find(WhitelistEntry.class, id);
        if (entry == null) return false;
        em.remove(entry);
        return true;
    }
}
