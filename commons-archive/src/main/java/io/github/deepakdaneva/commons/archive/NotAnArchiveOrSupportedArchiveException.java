/*
 * Copyright (C) 2023 Deepak Kumar Jangir
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.github.deepakdaneva.commons.archive;

/**
 * Thrown when content is not an archive, or is an archive/compression format this library cannot read (for example
 * RAR, or content whose signature does not match the declared MIME type).
 * <p>
 * This is an unchecked exception so that callers can handle it separately from I/O failures.
 *
 * @author Deepak Kumar Jangir
 * @since 1.0.0
 */
public class NotAnArchiveOrSupportedArchiveException extends RuntimeException {

    // Same value as the implicit serialVersionUID of 1.0.0, so serialized instances stay compatible.
    private static final long serialVersionUID = -6933252307440653469L;

    /**
     * Creates an exception with the given message.
     *
     * @param message detail message.
     */
    public NotAnArchiveOrSupportedArchiveException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given message and cause.
     *
     * @param message detail message.
     * @param throwable underlying cause, may be {@code null}.
     */
    public NotAnArchiveOrSupportedArchiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
