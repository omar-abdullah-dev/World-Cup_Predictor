package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.RoundStatus;
import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import com.worldcup.service.TournamentRoundService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class AdminRoundBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private TournamentRoundService roundService;
    @Inject private AuthBean authBean;

    private List<TournamentRound> rounds;
    private TournamentRound currentRound;

    @PostConstruct
    public void init() {
        loadRounds();
    }

    public void loadRounds() {
        rounds = roundService.getAllRounds();
    }

    public void prepareCreate() {
        currentRound = new TournamentRound();
    }

    public void save() {
        try {
            if (currentRound.getId() == null) {
                currentRound.setStatus(RoundStatus.OPEN);
                roundService.createRound(authBean.getUser(), currentRound);
                addMessage(FacesMessage.SEVERITY_INFO, "Round created successfully");
            } else {
                // Save changes (not heavily supported yet except status/deadline via other methods)
            }
            loadRounds();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void setStatus(Long id, RoundStatus status) {
        try {
            if (status == RoundStatus.OPEN) {
                roundService.openRound(authBean.getUser(), id);
            } else if (status == RoundStatus.LOCKED) {
                roundService.lockRound(authBean.getUser(), id);
            } else if (status == RoundStatus.CLOSED) {
                roundService.closeRound(authBean.getUser(), id);
            }
            addMessage(FacesMessage.SEVERITY_INFO, "Round status updated to " + status);
            loadRounds();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    public List<TournamentRound> getRounds() { return rounds; }
    public TournamentRound getCurrentRound() { return currentRound; }
    public void setCurrentRound(TournamentRound currentRound) { this.currentRound = currentRound; }
    public TournamentStage[] getStages() { return TournamentStage.values(); }
    public RoundStatus[] getStatuses() { return RoundStatus.values(); }
}
