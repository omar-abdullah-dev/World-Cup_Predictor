package com.worldcup.model;

import java.time.LocalDateTime;

/**
 * Plain Java model: Represents a tournament round (e.g. GROUP_STAGE, QUARTER_FINAL).
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaTournamentRoundRepository} via pure JDBC.
 */
public class TournamentRound {

    private Long id;
    private TournamentStage stage;
    private RoundStatus status = RoundStatus.UPCOMING;

    private LocalDateTime openedAt;
    private LocalDateTime predictionDeadline;
    private LocalDateTime lockedAt;
    private LocalDateTime closedAt;

    public TournamentRound() {}

    // ── Business logic ────────────────────────────────────────────────────

    public boolean isPredictionsAllowed() {
        return status == RoundStatus.OPEN
                && (predictionDeadline == null
                    || LocalDateTime.now().isBefore(predictionDeadline));
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                                         { return id; }
    public void setId(Long id)                                  { this.id = id; }

    public TournamentStage getStage()                           { return stage; }
    public void setStage(TournamentStage stage)                 { this.stage = stage; }

    public RoundStatus getStatus()                              { return status; }
    public void setStatus(RoundStatus status)                   { this.status = status; }

    public LocalDateTime getOpenedAt()                          { return openedAt; }
    public void setOpenedAt(LocalDateTime d)                    { this.openedAt = d; }

    public LocalDateTime getPredictionDeadline()                { return predictionDeadline; }
    public void setPredictionDeadline(LocalDateTime d)          { this.predictionDeadline = d; }

    public LocalDateTime getLockedAt()                          { return lockedAt; }
    public void setLockedAt(LocalDateTime d)                    { this.lockedAt = d; }

    public LocalDateTime getClosedAt()                          { return closedAt; }
    public void setClosedAt(LocalDateTime d)                    { this.closedAt = d; }
}
