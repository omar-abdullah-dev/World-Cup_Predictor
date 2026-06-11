package com.worldcup.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity: Represents a football match in the World Cup tournament.
 *
 * Tracks:
 * - Teams involved (home and away)
 * - Match timing (kickoff date)
 * - Scores and match status
 * - Result recording window (4 hours after kickoff)
 */
@Entity
@Table(name = "matches")
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int RESULT_WINDOW_HOURS = 4;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @Column(name = "kickoff_date", nullable = false)
    private LocalDateTime kickoffDate;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    public Match() {
        this.status = MatchStatus.SCHEDULED;
    }

    public Match(Long id, String homeTeam, String awayTeam, LocalDateTime kickoffDate) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffDate = kickoffDate;
        this.status = MatchStatus.SCHEDULED;
    }

    public Match(String homeTeam, String awayTeam, LocalDateTime kickoffDate) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffDate = kickoffDate;
        this.status = MatchStatus.SCHEDULED;
    }

    public boolean hasStarted() {
        return kickoffDate != null && !LocalDateTime.now().isBefore(kickoffDate);
    }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public boolean isPredictionOpen() {
        return status == MatchStatus.SCHEDULED && kickoffDate != null && !hasStarted();
    }

    public boolean isWithinResultWindow() {
        if (kickoffDate == null || isFinished()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(kickoffDate)
                && !now.isAfter(kickoffDate.plusHours(RESULT_WINDOW_HOURS));
    }

    public boolean canRecordResult() {
        return status == MatchStatus.SCHEDULED && isWithinResultWindow();
    }

    public LocalDateTime getResultDeadline() {
        return kickoffDate == null ? null : kickoffDate.plusHours(RESULT_WINDOW_HOURS);
    }

    public void finish(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = MatchStatus.FINISHED;
    }

    public Integer getOutcome() {
        if (homeScore == null || awayScore == null) return null;
        return Integer.compare(homeScore, awayScore);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public LocalDateTime getKickoffDate() { return kickoffDate; }
    public void setKickoffDate(LocalDateTime kickoffDate) { this.kickoffDate = kickoffDate; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Match) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Match{" + "id=" + id + ", homeTeam='" + homeTeam + '\'' +
               ", awayTeam='" + awayTeam + '\'' + ", kickoffDate=" + kickoffDate +
               ", status=" + status + '}';
    }
}
