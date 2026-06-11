package com.worldcup.repository;

import com.worldcup.model.Prediction;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository {
    Prediction save(Prediction prediction);
    Optional<Prediction> findById(Long id);
    Optional<Prediction> findByUserAndMatch(Long userId, Long matchId);
    List<Prediction> findByMatch(Long matchId);
    List<Prediction> findAll();
}
