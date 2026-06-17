package com.worldcup.api.dto;

public class ScoresDto {

    private String score;
    private String htScore;
    private String ftScore;
    private String etScore;
    private String psScore;

    public ScoresDto() {}

    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }

    public String getHtScore() { return htScore; }
    public void setHtScore(String htScore) { this.htScore = htScore; }

    public String getFtScore() { return ftScore; }
    public void setFtScore(String ftScore) { this.ftScore = ftScore; }

    public String getEtScore() { return etScore; }
    public void setEtScore(String etScore) { this.etScore = etScore; }

    public String getPsScore() { return psScore; }
    public void setPsScore(String psScore) { this.psScore = psScore; }
}
