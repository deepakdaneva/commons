package io.github.deepakdaneva.commons.archive;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class NotAnArchiveOrSupportedArchiveException extends RuntimeException {

    public NotAnArchiveOrSupportedArchiveException(String message) {
        super(message);
    }

    public NotAnArchiveOrSupportedArchiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
