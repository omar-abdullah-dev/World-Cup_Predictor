package com.worldcup.repository;



/**
 * Thread-safe in-memory Prediction store.
 */
/* DEPRECATED - Consolidating to JPA-only. Keep file for reference.
@ApplicationScoped
public class InMemoryPredictionRepository implements PredictionRepository {


    private final ConcurrentHashMap<Long, Prediction> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Prediction save(Prediction prediction) {
        if (prediction.getId() == null) prediction.setId(idGenerator.getAndIncrement());
        store.put(prediction.getId(), prediction);
        return prediction;
    }

    @Override
    public Optional<Prediction> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Prediction> findByUserAndMatch(Long userId, Long matchId) {
        return store.values().stream()
                .filter(p -> p.getUserId().equals(userId) && p.getMatchId().equals(matchId))
                .findFirst();
    }

    @Override
    public List<Prediction> findByMatch(Long matchId) {
        return store.values().stream()
                .filter(p -> p.getMatchId().equals(matchId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Prediction> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }
}
*/
