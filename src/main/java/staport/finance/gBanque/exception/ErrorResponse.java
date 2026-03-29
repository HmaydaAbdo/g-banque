package staport.finance.gBanque.exception;

import java.time.Instant;

/**
 * Unified error response record used by all module-level {@code @ControllerAdvice} handlers.
 *
 * <ul>
 *   <li>{@code status}    — HTTP status code (e.g. 404, 409)</li>
 *   <li>{@code error}     — short error label matching the HTTP status reason phrase</li>
 *   <li>{@code message}   — human-readable description shown to API clients</li>
 *   <li>{@code traces}    — optional internal detail (exception class, cause) — null in production</li>
 *   <li>{@code timestamp} — UTC instant the error occurred</li>
 * </ul>
 *
 * The global {@link GlobalExceptionHandler} still returns {@code Map<String,String>} for
 * security and generic fallback cases; it will be migrated to this record in a dedicated session.
 */
public record ErrorResponse(
        int     status,
        String  error,
        String  message,
        String  traces,
        Instant timestamp
) {
    /** Convenience factory — sets timestamp to now and traces to null. */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, null, Instant.now());
    }

    /** Convenience factory — includes exception class name as traces. */
    public static ErrorResponse of(int status, String error, String message, Exception ex) {
        String traces = ex.getClass().getSimpleName() +
                (ex.getCause() != null ? " caused by " + ex.getCause().getClass().getSimpleName() : "");
        return new ErrorResponse(status, error, message, traces, Instant.now());
    }
}
