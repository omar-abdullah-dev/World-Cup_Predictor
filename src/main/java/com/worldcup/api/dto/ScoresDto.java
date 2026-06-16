package com.worldcup.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoresDto {
    @JsonProperty("score")    private String score;
    @JsonProperty("ht_score") private String htScore;
    @JsonProperty("ft_score") private String ftScore;
    @JsonProperty("et_score") private String etScore;
    @JsonProperty("ps_score") private String psScore;

    public String getScore()   { return score == null ? "" : score; }
    public String getHtScore() { return htScore == null ? "" : htScore; }
    public String getFtScore() { return ftScore == null ? "" : ftScore; }
    public String getEtScore() { return etScore == null ? "" : etScore; }
    public String getPsScore() { return psScore == null ? "" : psScore; }

    public void setScore(String s)   { this.score = s; }
    public void setHtScore(String s) { this.htScore = s; }
    public void setFtScore(String s) { this.ftScore = s; }
    public void setEtScore(String s) { this.etScore = s; }
    public void setPsScore(String s) { this.psScore = s; }
}
