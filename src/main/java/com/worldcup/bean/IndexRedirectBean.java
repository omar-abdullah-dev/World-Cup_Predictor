package com.worldcup.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;

/**
 * Handles pre-render redirect for index.xhtml.
 * If the user is not authenticated, redirects to login page.
 */
@Named
@RequestScoped
public class IndexRedirectBean {

    @Inject private AuthBean authBean;

    public void checkAuth() {
        if (authBean == null || !authBean.isLoggedIn()) {
            try {
                FacesContext ctx = FacesContext.getCurrentInstance();
                String loginUrl = ctx.getExternalContext().getRequestContextPath() + "/login.xhtml";
                ctx.getExternalContext().redirect(loginUrl);
                ctx.responseComplete();
            } catch (IOException e) {
                // Ignore redirect failures
            }
        }
    }
}
