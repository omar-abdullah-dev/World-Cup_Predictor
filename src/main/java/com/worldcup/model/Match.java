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

    // We'll keep these String fields for fallback or denormalized view,
    // but the primary association will be the Team entities.
    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeamEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeamEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private TournamentRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage")
    private TournamentStage stage;

    @Column(name = "match_number")
    private Integer matchNumber;

    @Column(name = "prediction_deadline")
    private LocalDateTime predictionDeadline;

    @Column(name = "kickoff_date", nullable = false)
    private LocalDateTime kickoffDate;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "result_entered_at")
    private LocalDateTime resultEnteredAt;

    @Column(name = "result_locked_at")
    private LocalDateTime resultLockedAt;

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
        if (status != MatchStatus.SCHEDULED || hasStarted()) return false;
        if (predictionDeadline != null && LocalDateTime.now().isAfter(predictionDeadline)) return false;
        if (round != null && !round.isPredictionsAllowed()) return false;
        return true;
    }

    /* DEPRECATED - New lock logic uses resultEnteredAt instead of kickoffDate
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
    */

    public boolean isResultLocked() {
        if (resultLockedAt != null) return true;
        if (resultEnteredAt != null) {
            return LocalDateTime.now().isAfter(resultEnteredAt.plusHours(RESULT_WINDOW_HOURS));
        }
        return false;
    }

    public boolean canEditResult() {
        return status == MatchStatus.FINISHED && !isResultLocked();
    }

    public boolean canRecordResult() {
        if (status == MatchStatus.SCHEDULED && hasStarted()) return true;
        if (status == MatchStatus.FINISHED && !isResultLocked()) return true;
        return false;
    }

    public void finish(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = MatchStatus.FINISHED;
        if (this.resultEnteredAt == null) {
            this.resultEnteredAt = LocalDateTime.now();
        }
    }

    public Integer getOutcome() {
        if (homeScore == null || awayScore == null) return null;
        return Integer.compare(homeScore, awayScore);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHomeTeam() {
        if (homeTeam != null && !homeTeam.isBlank()) {
            return homeTeam;
        }
        return homeTeamEntity != null ? homeTeamEntity.getName() : "";
    }

    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() {
        if (awayTeam != null && !awayTeam.isBlank()) {
            return awayTeam;
        }
        return awayTeamEntity != null ? awayTeamEntity.getName() : "";
    }

    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public LocalDateTime getKickoffDate() { return kickoffDate; }
    public void setKickoffDate(LocalDateTime kickoffDate) { this.kickoffDate = kickoffDate; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public Team getHomeTeamEntity() { return homeTeamEntity; }
    public void setHomeTeamEntity(Team homeTeamEntity) { 
        this.homeTeamEntity = homeTeamEntity; 
        if (homeTeamEntity != null) this.homeTeam = homeTeamEntity.getName();
    }

    public Team getAwayTeamEntity() { return awayTeamEntity; }
    public void setAwayTeamEntity(Team awayTeamEntity) { 
        this.awayTeamEntity = awayTeamEntity; 
        if (awayTeamEntity != null) this.awayTeam = awayTeamEntity.getName();
    }

    public TournamentRound getRound() { return round; }
    public void setRound(TournamentRound round) { this.round = round; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public TournamentStage getStage() { return stage; }
    public void setStage(TournamentStage stage) { this.stage = stage; }

    public Integer getMatchNumber() { return matchNumber; }
    public void setMatchNumber(Integer matchNumber) { this.matchNumber = matchNumber; }

    public LocalDateTime getPredictionDeadline() { return predictionDeadline; }
    public void setPredictionDeadline(LocalDateTime predictionDeadline) { this.predictionDeadline = predictionDeadline; }

    public LocalDateTime getResultEnteredAt() { return resultEnteredAt; }
    public void setResultEnteredAt(LocalDateTime resultEnteredAt) { this.resultEnteredAt = resultEnteredAt; }

    public LocalDateTime getResultLockedAt() { return resultLockedAt; }
    public void setResultLockedAt(LocalDateTime resultLockedAt) { this.resultLockedAt = resultLockedAt; }

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
