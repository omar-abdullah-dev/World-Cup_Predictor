package com.worldcup.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Plain Java model: Represents a user's prediction for a specific football match.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaPredictionRepository} via pure JDBC.
 */
public class Prediction implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long matchId;
    private int predictedHomeScore;
    private int predictedAwayScore;
    private int earnedPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────

    public Prediction() {
        this.earnedPoints = 0;
        this.createdAt    = LocalDateTime.now();
        this.updatedAt    = this.createdAt;
    }

    public Prediction(Long id, Long userId, Long matchId,
                      int predictedHomeScore, int predictedAwayScore) {
        this.id                  = id;
        this.userId              = userId;
        this.matchId             = matchId;
        this.predictedHomeScore  = predictedHomeScore;
        this.predictedAwayScore  = predictedAwayScore;
        this.earnedPoints        = 0;
        this.createdAt           = LocalDateTime.now();
        this.updatedAt           = this.createdAt;
    }

    public Prediction(Long userId, Long matchId,
                      int predictedHomeScore, int predictedAwayScore) {
        this(null, userId, matchId, predictedHomeScore, predictedAwayScore);
    }

    // ── Business logic ────────────────────────────────────────────────────

    /** Returns +1 (home win predicted), 0 (draw), or -1 (away win predicted). */
    public int getPredictedOutcome() {
        return Integer.compare(predictedHomeScore, predictedAwayScore);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }

    public Long getUserId()                             { return userId; }
    public void setUserId(Long userId)                  { this.userId = userId; }

    public Long getMatchId()                            { return matchId; }
    public void setMatchId(Long matchId)                { this.matchId = matchId; }

    public int getPredictedHomeScore()                  { return predictedHomeScore; }
    public void setPredictedHomeScore(int s)            { this.predictedHomeScore = s; }

    public int getPredictedAwayScore()                  { return predictedAwayScore; }
    public void setPredictedAwayScore(int s)            { this.predictedAwayScore = s; }

    public int getEarnedPoints()                        { return earnedPoints; }
    public void setEarnedPoints(int p)                  { this.earnedPoints = p; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime d)           { this.createdAt = d; }

    public LocalDateTime getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(LocalDateTime d)           { this.updatedAt = d; }

    // ── Object contract ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Prediction) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Prediction{id=" + id + ", userId=" + userId + ", matchId=" + matchId
                + ", predicted=" + predictedHomeScore + "-" + predictedAwayScore
                + ", earnedPoints=" + earnedPoints + "}";
    }
}
