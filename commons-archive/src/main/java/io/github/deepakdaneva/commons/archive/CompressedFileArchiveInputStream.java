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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * One-entry {@link ArchiveInputStream} view of a compressed single file that is not a TAR archive, for example
 * {@code notes.txt.gz}.
 * <p>
 * {@link ArchiveUtil#getArchiveInputStream(InputStream, String)} returns an instance of this class when a compressed
 * stream ({@code gzip}, {@code bzip2}, {@code xz}, ...) does not wrap a TAR archive, so that such content can be read
 * with the same {@code getNextEntry()} / {@code read()} loop as any other archive. Callers that need to tell the two
 * cases apart can test the returned stream with {@code instanceof CompressedFileArchiveInputStream}.
 * <p>
 * The first call to {@link #getNextEntry()} returns the single entry and every later call returns {@code null}. The
 * entry content is the decompressed payload; it is streamed, not buffered, so size limits such as
 * {@link ArchiveUtil#getEntryInputStream(InputStream, long)} apply as usual. The entry has the following properties:
 * <ul>
 * <li>{@link ArchiveEntry#getName() name}: the original file name stored in the compressed header if the format records
 * one (only {@code gzip} does, and only when the producer wrote it), otherwise {@code null}.</li>
 * <li>{@link ArchiveEntry#getSize() size}: always {@link ArchiveEntry#SIZE_UNKNOWN}, because compressed streams do not
 * reliably record the uncompressed size.</li>
 * <li>{@link ArchiveEntry#getLastModifiedDate() last modified date}: always {@code null} (unknown).</li>
 * <li>{@link ArchiveEntry#isDirectory() directory}: always {@code false}.</li>
 * </ul>
 * Closing this stream closes the decompressing stream and with it the original input stream.
 *
 * @author Deepak Kumar Jangir
 * @since 1.1.0
 */
public final class CompressedFileArchiveInputStream extends ArchiveInputStream<ArchiveEntry> {

    private final InputStream decompressed;

    private final ArchiveEntry entry;

    /** {@code true} once the single entry has been returned by {@link #getNextEntry()}. */
    private boolean entryReturned;

    /** {@code true} while the single entry is the current entry and its content can be read. */
    private boolean entryActive;

    /**
     * Creates the one-entry view.
     *
     * @param decompressed decompressed content of the single file.
     * @param name entry name, may be {@code null}.
     */
    CompressedFileArchiveInputStream(final InputStream decompressed, final String name) {
        this.decompressed = Objects.requireNonNull(decompressed, "decompressed");
        this.entry = new CompressedFileEntry(name);
    }

    /**
     * Returns the single entry on the first call and {@code null} afterwards.
     *
     * @return the single entry, or {@code null} if it has already been returned.
     */
    @Override
    public ArchiveEntry getNextEntry() {
        if (entryReturned) {
            entryActive = false;
            return null;
        }
        entryReturned = true;
        entryActive = true;
        return entry;
    }

    /**
     * Reads the next byte of the decompressed content.
     *
     * @return the byte read, or {@code -1} at the end of the content or when no entry is current.
     * @throws IOException if decompression fails.
     */
    @Override
    public int read() throws IOException {
        if (!entryActive) {
            return -1;
        }
        final int b = decompressed.read();
        if (b >= 0) {
            count(1);
        }
        return b;
    }

    /**
     * Reads bytes of the decompressed content into the given array.
     *
     * @param b destination buffer.
     * @return the number of bytes read, or {@code -1} at the end of the content or when no entry is current.
     * @throws IOException if decompression fails.
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code len} bytes of the decompressed content into the given array.
     *
     * @param b destination buffer.
     * @param off offset in {@code b} at which to start writing.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or {@code -1} at the end of the content or when no entry is current.
     * @throws IOException if decompression fails.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (!entryActive) {
            return -1;
        }
        final int n = decompressed.read(b, off, len);
        if (n > 0) {
            count(n);
        }
        return n;
    }

    /**
     * Skips over bytes of the decompressed content.
     *
     * @param n number of bytes to skip.
     * @return the number of bytes actually skipped.
     * @throws IOException if decompression fails.
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n <= 0 || !entryActive) {
            return 0;
        }
        final long skipped = decompressed.skip(n);
        count(skipped);
        return skipped;
    }

    /**
     * Returns an estimate of the number of decompressed bytes that can be read without blocking.
     *
     * @return the estimate, {@code 0} when no entry is current.
     * @throws IOException if the underlying stream fails.
     */
    @Override
    public int available() throws IOException {
        return entryActive ? decompressed.available() : 0;
    }

    /**
     * Mark/reset is not supported.
     *
     * @return {@code false}.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Closes the decompressing stream, which also closes the original input stream.
     *
     * @throws IOException if closing fails.
     */
    @Override
    public void close() throws IOException {
        entryActive = false;
        decompressed.close();
    }

    /** The single entry of a {@link CompressedFileArchiveInputStream}. */
    private static final class CompressedFileEntry implements ArchiveEntry {

        private final String name;

        CompressedFileEntry(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getSize() {
            return SIZE_UNKNOWN;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public Date getLastModifiedDate() {
            return null;
        }

        @Override
        public String toString() {
            return "CompressedFileEntry[name=" + name + "]";
        }
    }
}
