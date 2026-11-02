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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;

/**
 * Streaming {@link ArchiveInputStream} view of a 7z archive.
 * <p>
 * The 7z format needs random access, so Commons Compress only offers it through {@link SevenZFile}. This class reads
 * the whole input into memory and exposes the familiar {@code getNextEntry()} / {@code read()} iteration on top of it,
 * which lets 7z archives be processed with the same code as zip or tar archives.
 * <p>
 * Because the archive is held in memory, prefer {@link #SevenZArchiveInputStream(InputStream, long)} with a size limit
 * for untrusted input.
 *
 * @author Deepak Kumar Jangir
 * @since 1.0.0
 */
public class SevenZArchiveInputStream extends ArchiveInputStream<ArchiveEntry> {

    private static final int SKIP_BUFFER_SIZE = 8192;

    private final SevenZFile sevenZFile;

    /**
     * Reads the whole 7z archive from the given stream into memory and opens it. The stream is fully consumed but not
     * closed.
     *
     * @param inputStream 7z archive content.
     * @throws IOException if the stream cannot be read or is not a valid 7z archive.
     * @throws org.apache.commons.compress.PasswordRequiredException if the archive headers are encrypted.
     * @throws NullPointerException if {@code inputStream} is {@code null}.
     */
    public SevenZArchiveInputStream(final InputStream inputStream) throws IOException {
        this(inputStream, Long.MAX_VALUE);
    }

    /**
     * Reads the whole 7z archive from the given stream into memory and opens it, refusing archives larger than
     * {@code maxArchiveSize} bytes. The stream is consumed but not closed.
     *
     * @param inputStream 7z archive content.
     * @param maxArchiveSize maximum number of bytes to buffer; must be zero or positive.
     * @throws IOException if the stream cannot be read, is larger than {@code maxArchiveSize} or is not a valid 7z
     * archive.
     * @throws org.apache.commons.compress.PasswordRequiredException if the archive headers are encrypted.
     * @throws NullPointerException if {@code inputStream} is {@code null}.
     * @throws IllegalArgumentException if {@code maxArchiveSize} is negative.
     * @since 1.1.0
     */
    public SevenZArchiveInputStream(final InputStream inputStream, final long maxArchiveSize) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        if (maxArchiveSize < 0) {
            throw new IllegalArgumentException("maxArchiveSize must not be negative: " + maxArchiveSize);
        }
        this.sevenZFile = SevenZFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(readFully(inputStream, maxArchiveSize))).get();
    }

    /**
     * Reads the next byte of the current entry.
     *
     * @return the byte read, or {@code -1} at the end of the current entry.
     * @throws IOException if reading fails.
     */
    @Override
    public int read() throws IOException {
        final int b = sevenZFile.read();
        if (b >= 0) {
            count(1);
        }
        return b;
    }

    /**
     * Reads bytes of the current entry into the given array.
     *
     * @param b destination buffer.
     * @return the number of bytes read, or {@code -1} at the end of the current entry.
     * @throws IOException if reading fails.
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code len} bytes of the current entry into the given array.
     *
     * @param b destination buffer.
     * @param off offset in {@code b} at which to start writing.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or {@code -1} at the end of the current entry.
     * @throws IOException if reading fails.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int n = sevenZFile.read(b, off, len);
        if (n > 0) {
            count(n);
        }
        return n;
    }

    /**
     * Skips over bytes of the current entry.
     *
     * @param n number of bytes to skip.
     * @return the number of bytes actually skipped.
     * @throws IOException if reading fails.
     * @since 1.1.0
     */
    @Override
    public long skip(final long n) throws IOException {
        // FilterInputStream.skip would delegate to the (empty) wrapped stream, so skip by reading the entry instead.
        if (n <= 0) {
            return 0;
        }
        final byte[] buffer = new byte[(int) Math.min(SKIP_BUFFER_SIZE, n)];
        long remaining = n;
        while (remaining > 0) {
            final int read = read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                break;
            }
            remaining -= read;
        }
        return n - remaining;
    }

    /**
     * Advances to the next entry of the archive.
     *
     * @return the next entry, or {@code null} if there are no more entries.
     * @throws IOException if the archive cannot be read.
     * @throws org.apache.commons.compress.PasswordRequiredException if the next entry is encrypted.
     */
    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        return sevenZFile.getNextEntry();
    }

    /**
     * Closes the underlying {@link SevenZFile} and releases the buffered archive.
     *
     * @throws IOException if closing fails.
     */
    @Override
    public void close() throws IOException {
        sevenZFile.close();
    }

    private static byte[] readFully(final InputStream inputStream, final long maxSize) throws IOException {
        if (maxSize == Long.MAX_VALUE) {
            return IOUtils.toByteArray(inputStream);
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // Read one byte beyond the limit so that an oversized archive can be told apart from one of exactly maxSize.
        if (IOUtils.copyLarge(inputStream, buffer, 0, maxSize + 1) > maxSize) {
            throw new IOException("7z archive exceeds the maximum allowed size of " + maxSize + " bytes");
        }
        return buffer.toByteArray();
    }
}
