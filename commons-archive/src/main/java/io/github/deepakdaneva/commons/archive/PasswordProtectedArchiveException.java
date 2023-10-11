package io.github.deepakdaneva.commons.archive;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class PasswordProtectedArchiveException extends RuntimeException {
    public PasswordProtectedArchiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
