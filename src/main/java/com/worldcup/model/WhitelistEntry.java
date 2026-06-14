package com.worldcup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whitelist")
public class WhitelistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String adUsername;

    private String employeeName;
    private String email;

    @Column(nullable = false)
    private boolean enabled = true;

    private LocalDateTime addedAt = LocalDateTime.now();

    private Long addedByUserId;

    public WhitelistEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdUsername() { return adUsername; }
    public void setAdUsername(String adUsername) { this.adUsername = adUsername; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public Long getAddedByUserId() { return addedByUserId; }
    public void setAddedByUserId(Long addedByUserId) { this.addedByUserId = addedByUserId; }
}
