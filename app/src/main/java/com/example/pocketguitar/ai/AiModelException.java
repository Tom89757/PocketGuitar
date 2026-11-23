package com.example.pocketguitar.ai;

/** Explicit model failure with a stable kind for UI decisions. */
public final class AiModelException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        NOT_CONFIGURED,
        INVALID_CONFIG,
        HTTP,
        TRANSPORT,
        PROTOCOL,
        EMPTY_RESPONSE
    }

    private final Kind kind;
    private final int statusCode;

    public AiModelException(Kind kind, String message) {
        this(kind, -1, message, null);
    }

    public AiModelException(Kind kind, String message, Throwable cause) {
        this(kind, -1, message, cause);
    }

    public AiModelException(Kind kind, int statusCode, String message) {
        this(kind, statusCode, message, null);
    }

    private AiModelException(Kind kind, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
        this.statusCode = statusCode;
    }

    public Kind kind() {
        return kind;
    }

    public int statusCode() {
        return statusCode;
    }
}
