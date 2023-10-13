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
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class NotAnArchiveOrSupportedArchiveException extends RuntimeException {

    /**
     * Constructor with exception message as a argument
     * 
     * @param message message of the exception
     */
    public NotAnArchiveOrSupportedArchiveException(String message) {
        super(message);
    }

    /**
     * Constructor with exception message and throwable instance as a argument
     * 
     * @param message message of the exception
     * @param throwable throwable instance of the exception
     */
    public NotAnArchiveOrSupportedArchiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
