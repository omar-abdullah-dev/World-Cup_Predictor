package com.worldcup.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

/**
 * UI helper for navigation highlighting in the layout template.
 */
@Named
@RequestScoped
public class NavBean {

    public boolean isActive(String page) {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if (viewId == null) {
            return false;
        }
        if ("index".equals(page)) {
            return viewId.endsWith("/index.xhtml");
        }
        if ("groups".equals(page)) {
            return viewId.contains("/groups.xhtml") || viewId.contains("/group-details.xhtml");
        }
        return viewId.contains("/" + page + ".xhtml");
    }
}
