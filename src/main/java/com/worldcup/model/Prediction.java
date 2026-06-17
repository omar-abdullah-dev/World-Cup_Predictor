package com.worldcup.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a user's prediction for a specific football match.
 * Uses userId/matchId references for compatibility with both in-memory and JPA repositories.
 */
@Entity
@Table(name = "predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "match_id"})
})
public class Prediction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "predicted_home_score", nullable = false)
    private int predictedHomeScore;

    @Column(name = "predicted_away_score", nullable = false)
    private int predictedAwayScore;

    @Column(name = "earned_points", nullable = false)
    private int earnedPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Prediction() {
        this.earnedPoints = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Prediction(Long id, Long userId, Long matchId, int predictedHomeScore, int predictedAwayScore) {
        this.id = id;
        this.userId = userId;
        this.matchId = matchId;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.earnedPoints = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Prediction(Long userId, Long matchId, int predictedHomeScore, int predictedAwayScore) {
        this(null, userId, matchId, predictedHomeScore, predictedAwayScore);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public int getPredictedOutcome() {
        return Integer.compare(predictedHomeScore, predictedAwayScore);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public int getPredictedHomeScore() { return predictedHomeScore; }
    public void setPredictedHomeScore(int predictedHomeScore) { this.predictedHomeScore = predictedHomeScore; }

    public int getPredictedAwayScore() { return predictedAwayScore; }
    public void setPredictedAwayScore(int predictedAwayScore) { this.predictedAwayScore = predictedAwayScore; }

    public int getEarnedPoints() { return earnedPoints; }
    public void setEarnedPoints(int earnedPoints) { this.earnedPoints = earnedPoints; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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
