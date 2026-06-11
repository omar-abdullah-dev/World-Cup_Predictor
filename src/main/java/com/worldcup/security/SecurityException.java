package com.worldcup.security;

/**
 * Exception thrown when a security check fails.
 */
public class SecurityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum SecurityErrorCode {
        AUTHENTICATION_FAILED,
        INSUFFICIENT_PERMISSION,
        USER_NOT_APPROVED,
        NOT_AUTHENTICATED,
        INVALID_TOKEN
    }

    private final SecurityErrorCode errorCode;

    public SecurityException(String message) {
        super(message);
        this.errorCode = SecurityErrorCode.AUTHENTICATION_FAILED;
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = SecurityErrorCode.AUTHENTICATION_FAILED;
    }

    public SecurityException(String message, SecurityErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SecurityErrorCode getErrorCode() { return errorCode; }

    public static SecurityException authenticationFailed(String reason) {
        return new SecurityException("Authentication failed: " + reason, SecurityErrorCode.AUTHENTICATION_FAILED);
    }

    public static SecurityException insufficientPermission(String operation, Role requiredRole) {
        return new SecurityException("Insufficient permission for '" + operation
            + "'. Required role: " + requiredRole.getCode(), SecurityErrorCode.INSUFFICIENT_PERMISSION);
    }

    public static SecurityException userNotApproved(String username) {
        return new SecurityException("User '" + username + "' has not been approved for system access. "
            + "Please contact an administrator.", SecurityErrorCode.USER_NOT_APPROVED);
    }

    public static SecurityException notAuthenticated() {
        return new SecurityException("User is not authenticated. Please log in.", SecurityErrorCode.NOT_AUTHENTICATED);
    }
}
