package com.worldcup.api.dto;

import java.time.LocalDateTime;

public class SyncResultDto {

    private final String operation;
    private final LocalDateTime syncTime;
    private int created;
    private int updated;
    private int skipped;
    private int errors;
    private String lastError;

    public SyncResultDto(String operation) {
        this.operation = operation;
        this.syncTime = LocalDateTime.now();
    }

    public void incrementCreated() { this.created++; }
    public void incrementUpdated() { this.updated++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors(String error) {
        this.errors++;
        this.lastError = error;
    }

    public String getOperation() { return operation; }
    public LocalDateTime getSyncTime() { return syncTime; }
    public int getCreated() { return created; }
    public int getUpdated() { return updated; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }
    public String getLastError() { return lastError; }
    public boolean isSuccess() { return errors == 0; }

    @Override
    public String toString() {
        return "SyncResultDto{operation='" + operation + "', created=" + created
                + ", updated=" + updated + ", skipped=" + skipped
                + ", errors=" + errors + ", lastError='" + lastError + "'}";
    }
}
