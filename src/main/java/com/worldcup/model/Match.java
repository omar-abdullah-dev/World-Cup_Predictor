package com.worldcup.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import com.worldcup.config.GameConstants;

/**
 * Plain Java model: Represents a football match in the World Cup tournament.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaMatchRepository} via pure JDBC.
 *
 * All business logic, constructors, and field names are unchanged.
 * Java 8 / WebLogic compatible — no Java 9+ language features.
 */
public class Match implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RESULT_WINDOW_HOURS = 4;

    private Long id;

    /** Denormalized home/away team names — fast display without joins. */
    private String homeTeam;
    private String awayTeam;

    /** Full Team entity references — populated by the JDBC repository via FK join. */
    private Team homeTeamEntity;
    private Team awayTeamEntity;

    private TournamentRound round;
    private Group group;
    private TournamentStage stage;
    private Integer matchNumber;
    private LocalDateTime predictionDeadline;
    private LocalDateTime kickoffDate;
    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime resultEnteredAt;
    private LocalDateTime resultLockedAt;
    private MatchStatus status;

    // ── Knockout round fields ─────────────────────────────────────────────

    /** Extra time home score (knockout rounds only). */
    private Integer extraTimeHomeScore;

    /** Extra time away score (knockout rounds only). */
    private Integer extraTimeAwayScore;

    /** Penalty shootout home score (knockout rounds only). */
    private Integer penaltyHomeScore;

    /** Penalty shootout away score (knockout rounds only). */
    private Integer penaltyAwayScore;

    /**
     * How the match was decided.
     *   "90"  = normal time (or null for group stage)
     *   "ET"  = extra time
     *   "PEN" = penalties
     */
    private String matchDecidedBy;

    // ── Constructors ──────────────────────────────────────────────────────

    public Match() {
        this.status = MatchStatus.SCHEDULED;
    }

    public Match(Long id, String homeTeam, String awayTeam, LocalDateTime kickoffDate) {
        this.id          = id;
        this.homeTeam    = homeTeam;
        this.awayTeam    = awayTeam;
        this.kickoffDate = kickoffDate;
        this.status      = MatchStatus.SCHEDULED;
    }

    public Match(String homeTeam, String awayTeam, LocalDateTime kickoffDate) {
        this.homeTeam    = homeTeam;
        this.awayTeam    = awayTeam;
        this.kickoffDate = kickoffDate;
        this.status      = MatchStatus.SCHEDULED;
    }

    // ── Business logic ────────────────────────────────────────────────────

    public boolean hasStarted() {
        return kickoffDate != null && !LocalDateTime.now().isBefore(kickoffDate);
    }

    public boolean isPredictionLocked() {
        if (kickoffDate == null) return false;
        LocalDateTime lockTime = kickoffDate.minusMinutes(GameConstants.PREDICTION_LOCK_MINUTES);
        return !LocalDateTime.now().isBefore(lockTime);
    }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public boolean isPredictionOpen() {
        if (status != MatchStatus.SCHEDULED) return false;
        if (isPredictionLocked()) return false;
        if (predictionDeadline != null && LocalDateTime.now().isAfter(predictionDeadline)) return false;
        if (round != null && !round.isPredictionsAllowed()) return false;
        return true;
    }

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

    public boolean isKnockoutMatch() {
        return stage != null && stage != TournamentStage.GROUP_STAGE;
    }

    public void finish(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status    = MatchStatus.FINISHED;
        if (this.resultEnteredAt == null) {
            this.resultEnteredAt = LocalDateTime.now();
        }
    }

    public Integer getOutcome() {
        if (homeScore == null || awayScore == null) return null;
        return Integer.compare(homeScore, awayScore);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()         { return id; }
    public void setId(Long id)  { this.id = id; }

    public String getHomeTeam() {
        if (homeTeam != null && !homeTeam.isEmpty()) return homeTeam;
        return homeTeamEntity != null ? homeTeamEntity.getName() : "";
    }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() {
        if (awayTeam != null && !awayTeam.isEmpty()) return awayTeam;
        return awayTeamEntity != null ? awayTeamEntity.getName() : "";
    }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public LocalDateTime getKickoffDate()                       { return kickoffDate; }
    public void setKickoffDate(LocalDateTime d)                 { this.kickoffDate = d; }

    public Integer getHomeScore()                               { return homeScore; }
    public void setHomeScore(Integer s)                         { this.homeScore = s; }

    public Integer getAwayScore()                               { return awayScore; }
    public void setAwayScore(Integer s)                         { this.awayScore = s; }

    public MatchStatus getStatus()                              { return status; }
    public void setStatus(MatchStatus status)                   { this.status = status; }

    public Team getHomeTeamEntity()                             { return homeTeamEntity; }
    public void setHomeTeamEntity(Team t) {
        this.homeTeamEntity = t;
        if (t != null) this.homeTeam = t.getName();
    }

    public Team getAwayTeamEntity()                             { return awayTeamEntity; }
    public void setAwayTeamEntity(Team t) {
        this.awayTeamEntity = t;
        if (t != null) this.awayTeam = t.getName();
    }

    public TournamentRound getRound()                           { return round; }
    public void setRound(TournamentRound round)                 { this.round = round; }

    public Group getGroup()                                     { return group; }
    public void setGroup(Group group)                           { this.group = group; }

    public TournamentStage getStage()                           { return stage; }
    public void setStage(TournamentStage stage)                 { this.stage = stage; }

    public Integer getMatchNumber()                             { return matchNumber; }
    public void setMatchNumber(Integer matchNumber)             { this.matchNumber = matchNumber; }

    public LocalDateTime getPredictionDeadline()                { return predictionDeadline; }
    public void setPredictionDeadline(LocalDateTime d)          { this.predictionDeadline = d; }

    public LocalDateTime getResultEnteredAt()                   { return resultEnteredAt; }
    public void setResultEnteredAt(LocalDateTime d)             { this.resultEnteredAt = d; }

    public LocalDateTime getResultLockedAt()                    { return resultLockedAt; }
    public void setResultLockedAt(LocalDateTime d)              { this.resultLockedAt = d; }

    public Integer getExtraTimeHomeScore()                      { return extraTimeHomeScore; }
    public void setExtraTimeHomeScore(Integer s)                { this.extraTimeHomeScore = s; }

    public Integer getExtraTimeAwayScore()                      { return extraTimeAwayScore; }
    public void setExtraTimeAwayScore(Integer s)                { this.extraTimeAwayScore = s; }

    public Integer getPenaltyHomeScore()                        { return penaltyHomeScore; }
    public void setPenaltyHomeScore(Integer s)                  { this.penaltyHomeScore = s; }

    public Integer getPenaltyAwayScore()                        { return penaltyAwayScore; }
    public void setPenaltyAwayScore(Integer s)                  { this.penaltyAwayScore = s; }

    public String getMatchDecidedBy()                           { return matchDecidedBy; }
    public void setMatchDecidedBy(String s)                     { this.matchDecidedBy = s; }

    // ── Object contract ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Match) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Match{id=" + id + ", homeTeam='" + homeTeam + "'"
                + ", awayTeam='" + awayTeam + "'"
                + ", kickoffDate=" + kickoffDate
                + ", status=" + status + '}';
    }
}
