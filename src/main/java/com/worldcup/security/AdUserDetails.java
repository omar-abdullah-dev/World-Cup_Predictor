package com.worldcup.security;

public class AdUserDetails {
    private String adUsername;
    private String displayName;
    private String email;
    private String employeeId;

    public AdUserDetails() {}

    public AdUserDetails(String adUsername, String displayName, String email, String employeeId) {
        this.adUsername = adUsername;
        this.displayName = displayName;
        this.email = email;
        this.employeeId = employeeId;
    }

    public String getAdUsername() { return adUsername; }
    public void setAdUsername(String adUsername) { this.adUsername = adUsername; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
}
