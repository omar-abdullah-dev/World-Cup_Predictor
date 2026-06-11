package com.worldcup.repository;

import com.worldcup.model.Match;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory Match store.
 */
@ApplicationScoped
public class InMemoryMatchRepository implements MatchRepository {

    private final ConcurrentHashMap<Long, Match> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Match save(Match match) {
        if (match.getId() == null) match.setId(idGenerator.getAndIncrement());
        store.put(match.getId(), match);
        return match;
    }

    @Override
    public Optional<Match> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Match> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public Match update(Match match) {
        if (match.getId() == null || !store.containsKey(match.getId()))
            throw new IllegalArgumentException("Cannot update non-existent match. ID: " + match.getId());
        store.put(match.getId(), match);
        return match;
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
