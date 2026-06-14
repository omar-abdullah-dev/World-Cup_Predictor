package com.worldcup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_rounds")
public class TournamentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private TournamentStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status = RoundStatus.UPCOMING;

    private LocalDateTime openedAt;
    
    // When predictions are no longer accepted
    private LocalDateTime predictionDeadline;
    private LocalDateTime lockedAt;
    
    // When all matches are finished and results finalized
    private LocalDateTime closedAt;

    public TournamentRound() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TournamentStage getStage() { return stage; }
    public void setStage(TournamentStage stage) { this.stage = stage; }

    public RoundStatus getStatus() { return status; }
    public void setStatus(RoundStatus status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getPredictionDeadline() { return predictionDeadline; }
    public void setPredictionDeadline(LocalDateTime predictionDeadline) { this.predictionDeadline = predictionDeadline; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public boolean isPredictionsAllowed() {
        return status == RoundStatus.OPEN && 
               (predictionDeadline == null || LocalDateTime.now().isBefore(predictionDeadline));
    }
}
