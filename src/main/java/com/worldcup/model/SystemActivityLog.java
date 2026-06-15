package com.worldcup.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity: Records a system activity log entry for each user interaction.
 *
 * Columns:
 *  opmaj       - the major operation performed (e.g. LOGIN, PREDICT, RESULT_ENTERED)
 *  datemaj     - timestamp of the operation
 *  transmaj    - transaction/detail description (free-text context)
 *  profilemaj  - username / profile identifier of the person who performed the action
 */
@Entity
@Table(name = "system_activity_log",
       indexes = {
           @Index(name = "idx_sal_profilemaj", columnList = "profilemaj"),
           @Index(name = "idx_sal_datemaj",    columnList = "datemaj"),
           @Index(name = "idx_sal_opmaj",      columnList = "opmaj")
       })
public class SystemActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Major operation name (e.g. LOGIN, LOGOUT, PREDICT, UPDATE_RESULT). */
    @Column(name = "opmaj", nullable = false, length = 100)
    private String opmaj;

    /** Timestamp when the operation occurred. */
    @Column(name = "datemaj", nullable = false)
    private LocalDateTime datemaj;

    /** Transaction detail / free-text description of what happened. */
    @Column(name = "transmaj", length = 500)
    private String transmaj;

    /** Username / profile identifier of the actor. */
    @Column(name = "profilemaj", nullable = false, length = 100)
    private String profilemaj;

    public SystemActivityLog() {
        this.datemaj = LocalDateTime.now();
    }

    public SystemActivityLog(String opmaj, String transmaj, String profilemaj) {
        this.opmaj      = opmaj;
        this.transmaj   = transmaj;
        this.profilemaj = profilemaj;
        this.datemaj    = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOpmaj() { return opmaj; }
    public void setOpmaj(String opmaj) { this.opmaj = opmaj; }

    public LocalDateTime getDatemaj() { return datemaj; }
    public void setDatemaj(LocalDateTime datemaj) { this.datemaj = datemaj; }

    public String getTransmaj() { return transmaj; }
    public void setTransmaj(String transmaj) { this.transmaj = transmaj; }

    public String getProfilemaj() { return profilemaj; }
    public void setProfilemaj(String profilemaj) { this.profilemaj = profilemaj; }

    @Override
    public String toString() {
        return "SystemActivityLog{id=" + id
                + ", opmaj='" + opmaj + '\''
                + ", datemaj=" + datemaj
                + ", profilemaj='" + profilemaj + '\''
                + ", transmaj='" + transmaj + '\'' + '}';
    }
}
