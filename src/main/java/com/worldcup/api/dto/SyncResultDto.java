package com.worldcup.api.dto;

import java.time.LocalDateTime;

/** Returned by every sync operation to report what happened. */
public class SyncResultDto {

    private final String operation;
    private final LocalDateTime syncTime;
    private int created;
    private int updated;
    private int skipped;
    private int errors;
    private String lastError;
    private boolean success;

    public SyncResultDto(String operation) {
        this.operation = operation;
        this.syncTime  = LocalDateTime.now();
        this.success   = true;
    }

    public void incrementCreated() { created++; }
    public void incrementUpdated() { updated++; }
    public void incrementSkipped() { skipped++; }
    public void incrementErrors(String error) {
        errors++;
        lastError = error;
        success = false;
    }

    public String getOperation()    { return operation; }
    public LocalDateTime getSyncTime() { return syncTime; }
    public int getCreated()         { return created; }
    public int getUpdated()         { return updated; }
    public int getSkipped()         { return skipped; }
    public int getErrors()          { return errors; }
    public String getLastError()    { return lastError; }
    public boolean isSuccess()      { return success; }

    @Override
    public String toString() {
        return operation + " | created=" + created + " updated=" + updated
                + " skipped=" + skipped + " errors=" + errors
                + (lastError != null ? " lastError=" + lastError : "");
    }
}
