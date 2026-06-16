package com.worldcup.api;

/** Thrown when the WorldCupAPI returns an error response or is unreachable. */
public class WorldCupApiException extends RuntimeException {

    private final int httpCode;

    public WorldCupApiException(String message) {
        super(message);
        this.httpCode = 0;
    }

    public WorldCupApiException(String message, int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    public WorldCupApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpCode = 0;
    }

    public int getHttpCode() { return httpCode; }
}
