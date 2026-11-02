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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;

/**
 * Static helpers for recognising archive / compressor MIME types and for opening the matching Apache Commons Compress
 * streams.
 * <p>
 * Supported inputs:
 * <ul>
 * <li><b>Archives</b> ({@code zip}, {@code jar}, {@code tar}, {@code ar}, {@code arj}, {@code cpio}, {@code dump}) are
 * opened with {@link ArchiveStreamFactory} using signature auto-detection.</li>
 * <li><b>7z</b> archives are opened with {@link SevenZArchiveInputStream}, which buffers the whole archive in memory
 * because the 7z format requires random access. Decoding 7z entries requires the optional {@code org.tukaani:xz}
 * library on the class path.</li>
 * <li><b>Compressed streams</b> ({@code gzip}, {@code bzip2}, {@code xz}, {@code lzma}, {@code brotli}, ...) are
 * decompressed with {@link CompressorStreamFactory} and the result is read as a TAR archive.</li>
 * <li><b>RAR</b> is recognised by {@link #isRarMimeType(String)} but cannot be opened (Commons Compress has no RAR
 * support); {@link #getArchiveInputStream(InputStream, String)} rejects it with
 * {@link NotAnArchiveOrSupportedArchiveException}.</li>
 * </ul>
 * <p>
 * MIME type arguments are matched case-insensitively and any parameters (for example {@code ;charset=UTF-8}) are
 * ignored. Encrypted content is reported with {@link PasswordProtectedArchiveException}; the one exception is an
 * encrypted 7z entry (with unencrypted headers), for which {@link ArchiveInputStream#getNextEntry()} already throws
 * Commons Compress' {@link PasswordRequiredException}.
 * <p>
 * This class never extracts entries to the file system, so it is not itself exposed to path traversal ("zip slip").
 * Callers that write entries to disk must validate each {@link org.apache.commons.compress.archivers.ArchiveEntry#getName()
 * entry name} against the target directory themselves.
 *
 * @author Deepak Kumar Jangir
 * @since 1.0.0
 */
public class ArchiveUtil {

    /** Separator between a MIME type and its parameters, for example {@code application/zip; charset=UTF-8}. */
    public static final String SEMICOLON = ";";

    /** MIME types of single-stream compression formats that are decompressed and then read as TAR archives. */
    public static final Set<String> COMPRESSOR_MIME_TYPES = Set.of("application/zlib", "application/x-gzip", "application/x-bzip2", "application/x-compress", "application/x-java-pack200", "application/x-lzma", "application/deflate64", "application/x-lz4", "application/x-snappy", "application/x-brotli", "application/gzip", "application/x-bzip", "application/x-xz");

    /** 7z MIME type. */
    private static final String SEVENZ_MIME_TYPE = "application/x-7z-compressed";

    /** RAR MIME type. */
    private static final String RAR_MIME_TYPE = "application/x-rar-compressed";

    /** MIME types of multi-entry archive formats (including 7z and RAR). */
    private static final Set<String> ARCHIVE_MIME_TYPES = Set.of("application/x-tar", "application/x-gtar", "application/java-archive", "application/x-arj", "application/x-archive", "application/zip", "application/x-cpio", "application/x-tika-unix-dump", SEVENZ_MIME_TYPE, RAR_MIME_TYPE);

    /** Message used for every "not an archive" failure. */
    private static final String NOT_AN_ARCHIVE_MESSAGE = "Not an archive or an unsupported archive file.";

    /** Message used for every password / encryption failure. */
    private static final String PASSWORD_PROTECTED_MESSAGE = "Password protected archive/entries not supported";

    // Commons Compress signals "unknown format" only through these exception messages.
    private static final String NO_ARCHIVER_FOUND = "No Archiver found for the stream signature";
    private static final String NO_COMPRESSOR_FOUND = "No Compressor found for the stream signature";
    private static final String BAD_7Z_SIGNATURE = "Bad 7z signature";

    /**
     * Creates a new instance.
     *
     * @deprecated this is a static utility class and does not need to be instantiated; the constructor is kept only for
     * backward compatibility.
     */
    @Deprecated
    public ArchiveUtil() {
        // Kept public for binary compatibility with 1.0.0.
    }

    /**
     * Tells whether the MIME type denotes an archive, a compressed stream or a RAR file.
     *
     * @param mimeType MIME type to check, may be {@code null}.
     * @return {@code true} if {@link #isArchiveMimeType(String)}, {@link #isCompressorMimeType(String)} or
     * {@link #isRarMimeType(String)} returns {@code true}.
     */
    public static boolean isAnyArchiveMimeType(final String mimeType) {
        return isArchiveMimeType(mimeType) || isCompressorMimeType(mimeType) || isRarMimeType(mimeType);
    }

    /**
     * Tells whether the MIME type denotes a multi-entry archive format (zip, jar, tar, ar, arj, cpio, dump, 7z or RAR).
     *
     * @param mimeType MIME type to check, may be {@code null}.
     * @return {@code true} if the MIME type is a known archive MIME type.
     */
    public static boolean isArchiveMimeType(final String mimeType) {
        return matches(ARCHIVE_MIME_TYPES, mimeType);
    }

    /**
     * Tells whether the MIME type denotes a single-stream compression format listed in {@link #COMPRESSOR_MIME_TYPES}.
     *
     * @param mimeType MIME type to check, may be {@code null}.
     * @return {@code true} if the MIME type is a known compressor MIME type.
     */
    public static boolean isCompressorMimeType(final String mimeType) {
        return matches(COMPRESSOR_MIME_TYPES, mimeType);
    }

    /**
     * Tells whether the MIME type is the RAR MIME type ({@code application/x-rar-compressed}).
     *
     * @param mimeType MIME type to check, may be {@code null}.
     * @return {@code true} if the MIME type denotes a RAR file.
     */
    public static boolean isRarMimeType(final String mimeType) {
        return RAR_MIME_TYPE.equals(baseMimeType(mimeType));
    }

    /**
     * Tells whether the MIME type is the 7z MIME type ({@code application/x-7z-compressed}).
     *
     * @param mimeType MIME type to check, may be {@code null}.
     * @return {@code true} if the MIME type denotes a 7z file.
     */
    public static boolean is7zMimeType(final String mimeType) {
        return SEVENZ_MIME_TYPE.equals(baseMimeType(mimeType));
    }

    /**
     * Returns the Commons Compress compressor name for Brotli or LZMA content. These two formats cannot be detected from
     * the stream signature, so the name has to be passed explicitly to
     * {@link #getCompressorInputStream(InputStream, String)}.
     *
     * @param mimeType MIME type to inspect, may be {@code null}.
     * @return {@link CompressorStreamFactory#getBrotli()} if the MIME type contains {@code brotli},
     * {@link CompressorStreamFactory#getLzma()} if it contains {@code lzma}, otherwise {@code null}.
     */
    public static String getBrotliOrLzmaName(final String mimeType) {
        final String base = baseMimeType(mimeType);
        if (base == null) {
            return null;
        } else if (base.contains("brotli")) {
            return CompressorStreamFactory.getBrotli();
        } else if (base.contains("lzma")) {
            return CompressorStreamFactory.getLzma();
        }
        return null;
    }

    /**
     * Opens an {@link ArchiveInputStream} over the given stream, choosing the reader from the MIME type.
     * <p>
     * If the stream does not support {@link InputStream#mark(int) mark/reset} (needed for signature detection) it is
     * wrapped in a {@link BufferedInputStream}. The caller remains responsible for closing both the returned stream and
     * the original stream. For 7z input the whole stream is read into memory before this method returns.
     *
     * @param inputStream archive content; should support mark/reset (for example a {@link BufferedInputStream}).
     * @param mimeType MIME type of the content, used to choose the archive reader.
     * @return a new {@link ArchiveInputStream}, never {@code null}.
     * @throws ArchiveException if Commons Compress fails to create the archive reader for another reason.
     * @throws IOException if reading the stream fails.
     * @throws CompressorException if the compressor stream cannot be created for another reason.
     * @throws NotAnArchiveOrSupportedArchiveException if the MIME type is not a supported archive/compressor type
     * (including RAR), or the content does not match a supported format.
     * @throws PasswordProtectedArchiveException if the 7z archive headers are encrypted.
     * @throws NullPointerException if {@code inputStream} is {@code null}.
     */
    @SuppressWarnings("rawtypes")
    public static ArchiveInputStream getArchiveInputStream(final InputStream inputStream, final String mimeType) throws ArchiveException, IOException, CompressorException, NotAnArchiveOrSupportedArchiveException {
        Objects.requireNonNull(inputStream, "inputStream");
        if (is7zMimeType(mimeType)) {
            return open7z(inputStream);
        }
        if (isArchiveMimeType(mimeType) && !isRarMimeType(mimeType)) {
            try {
                return ArchiveStreamFactory.DEFAULT.createArchiveInputStream(markSupporting(inputStream));
            } catch (ArchiveException ae) {
                if (messageStartsWith(ae, NO_ARCHIVER_FOUND)) {
                    throw new NotAnArchiveOrSupportedArchiveException(NOT_AN_ARCHIVE_MESSAGE, ae);
                }
                throw ae;
            }
        }
        if (isCompressorMimeType(mimeType)) {
            return new TarArchiveInputStream(getCompressorInputStream(inputStream, getBrotliOrLzmaName(mimeType)));
        }
        throw new NotAnArchiveOrSupportedArchiveException(NOT_AN_ARCHIVE_MESSAGE + " MIME type: " + mimeType);
    }

    /**
     * Opens a {@link CompressorInputStream} over the given stream.
     * <p>
     * When {@code compressorName} is {@code null} the format is detected from the stream signature; in that case a
     * stream without mark/reset support is wrapped in a {@link BufferedInputStream}.
     *
     * @param inputStream compressed content; should support mark/reset (for example a {@link BufferedInputStream}).
     * @param compressorName optional Commons Compress compressor name (see {@link CompressorStreamFactory}); use it for
     * formats without a signature such as Brotli or LZMA. May be {@code null} to auto-detect.
     * @return a new {@link CompressorInputStream}, never {@code null}.
     * @throws NotAnArchiveOrSupportedArchiveException if no compressor matches the stream signature.
     * @throws CompressorException if the compressor stream cannot be created for another reason (for example an unknown
     * {@code compressorName} or a missing optional dependency).
     * @throws NullPointerException if {@code inputStream} is {@code null}.
     */
    public static CompressorInputStream getCompressorInputStream(final InputStream inputStream, final String compressorName) throws CompressorException {
        Objects.requireNonNull(inputStream, "inputStream");
        final CompressorStreamFactory factory = CompressorStreamFactory.getSingleton();
        try {
            if (compressorName != null) {
                return factory.createCompressorInputStream(compressorName, inputStream);
            }
            return factory.createCompressorInputStream(markSupporting(inputStream));
        } catch (CompressorException ce) {
            if (messageStartsWith(ce, NO_COMPRESSOR_FOUND)) {
                throw new NotAnArchiveOrSupportedArchiveException(NOT_AN_ARCHIVE_MESSAGE, ce);
            }
            throw ce;
        }
    }

    /**
     * Same as {@link #getArchiveInputStream(InputStream, String)}, but can return {@code null} instead of throwing.
     * <p>
     * Note that on failure the input stream may already have been partially consumed.
     *
     * @param inputStream archive content; should support mark/reset (for example a {@link BufferedInputStream}).
     * @param mimeType MIME type of the content, used to choose the archive reader.
     * @param ignoreException if {@code true}, any exception is swallowed and {@code null} is returned; if
     * {@code false}, exceptions are propagated.
     * @return a new {@link ArchiveInputStream}, or {@code null} if it could not be created and
     * {@code ignoreException} is {@code true}.
     * @throws CompressorException see {@link #getArchiveInputStream(InputStream, String)}.
     * @throws IOException see {@link #getArchiveInputStream(InputStream, String)}.
     * @throws ArchiveException see {@link #getArchiveInputStream(InputStream, String)}.
     * @throws NotAnArchiveOrSupportedArchiveException see {@link #getArchiveInputStream(InputStream, String)}.
     * @throws PasswordProtectedArchiveException see {@link #getArchiveInputStream(InputStream, String)}.
     */
    @SuppressWarnings("rawtypes")
    public static ArchiveInputStream getArchiveInputStream(final InputStream inputStream, final String mimeType, final boolean ignoreException) throws CompressorException, IOException, ArchiveException {
        try {
            return getArchiveInputStream(inputStream, mimeType);
        } catch (Exception e) {
            if (!ignoreException) {
                throw e;
            }
            return null;
        }
    }

    /**
     * Reads the current entry of an archive stream fully into memory and returns it as a new stream.
     * <p>
     * The entry size is not limited; for untrusted input prefer {@link #getEntryInputStream(InputStream, long)} to
     * guard against decompression bombs.
     *
     * @param currentArcInStream archive stream positioned at the entry to read (after
     * {@link ArchiveInputStream#getNextEntry()}).
     * @return a {@link BufferedInputStream} over the entry content, never {@code null}.
     * @throws IOException if reading the entry fails, including when the entry uses an unsupported feature such as an
     * unknown compression method.
     * @throws PasswordProtectedArchiveException if the entry is encrypted.
     * @throws NullPointerException if {@code currentArcInStream} is {@code null}.
     */
    public static InputStream getEntryInputStream(final InputStream currentArcInStream) throws IOException {
        return getEntryInputStream(currentArcInStream, Long.MAX_VALUE);
    }

    /**
     * Reads the current entry of an archive stream into memory, failing if it is larger than {@code maxSize} bytes.
     *
     * @param currentArcInStream archive stream positioned at the entry to read (after
     * {@link ArchiveInputStream#getNextEntry()}).
     * @param maxSize maximum number of uncompressed bytes to read; must be zero or positive.
     * @return a {@link BufferedInputStream} over the entry content, never {@code null}.
     * @throws IOException if reading the entry fails, the entry uses an unsupported feature, or the entry is larger
     * than {@code maxSize} bytes.
     * @throws PasswordProtectedArchiveException if the entry is encrypted.
     * @throws NullPointerException if {@code currentArcInStream} is {@code null}.
     * @throws IllegalArgumentException if {@code maxSize} is negative.
     * @since 1.1.0
     */
    public static InputStream getEntryInputStream(final InputStream currentArcInStream, final long maxSize) throws IOException {
        Objects.requireNonNull(currentArcInStream, "currentArcInStream");
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must not be negative: " + maxSize);
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            // Read one byte beyond the limit so that an oversized entry can be told apart from one of exactly maxSize.
            final long limit = maxSize == Long.MAX_VALUE ? -1 : maxSize + 1;
            final long copied = IOUtils.copyLarge(currentArcInStream, buffer, 0, limit);
            if (copied > maxSize) {
                throw new IOException("Archive entry exceeds the maximum allowed size of " + maxSize + " bytes");
            }
        } catch (UnsupportedZipFeatureException e) {
            if (UnsupportedZipFeatureException.Feature.ENCRYPTION.equals(e.getFeature())) {
                throw new PasswordProtectedArchiveException(PASSWORD_PROTECTED_MESSAGE, e);
            }
            throw e;
        } catch (PasswordRequiredException e) {
            throw new PasswordProtectedArchiveException(PASSWORD_PROTECTED_MESSAGE, e);
        }
        return new BufferedInputStream(new ByteArrayInputStream(buffer.toByteArray()));
    }

    /**
     * Closes every stream in the list, ignoring {@link IOException}s and {@code null} elements.
     *
     * @param aisList streams to close, may be {@code null}.
     */
    @SuppressWarnings("rawtypes")
    public static void closeQuietly(final List<ArchiveInputStream> aisList) {
        if (aisList == null) {
            return;
        }
        for (final ArchiveInputStream ais : aisList) {
            IOUtils.closeQuietly(ais);
        }
    }

    /** Opens a 7z archive and translates Commons Compress failures into this library's exceptions. */
    private static SevenZArchiveInputStream open7z(final InputStream inputStream) throws IOException {
        try {
            return new SevenZArchiveInputStream(inputStream);
        } catch (PasswordRequiredException e) {
            throw new PasswordProtectedArchiveException(PASSWORD_PROTECTED_MESSAGE, e);
        } catch (EOFException e) {
            // Input shorter than the 7z signature header.
            throw new NotAnArchiveOrSupportedArchiveException(NOT_AN_ARCHIVE_MESSAGE, e);
        } catch (IOException e) {
            if (messageStartsWith(e, BAD_7Z_SIGNATURE)) {
                throw new NotAnArchiveOrSupportedArchiveException(NOT_AN_ARCHIVE_MESSAGE, e);
            }
            throw e;
        }
    }

    /**
     * Returns the MIME type without parameters, trimmed and lower-cased, or {@code null} if {@code mimeType} is
     * {@code null}.
     */
    private static String baseMimeType(final String mimeType) {
        if (mimeType == null) {
            return null;
        }
        final int separator = mimeType.indexOf(SEMICOLON);
        final String base = separator < 0 ? mimeType : mimeType.substring(0, separator);
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /** Null-safe membership test; immutable {@code Set.of} sets reject {@code contains(null)}. */
    private static boolean matches(final Set<String> mimeTypes, final String mimeType) {
        final String base = baseMimeType(mimeType);
        return base != null && mimeTypes.contains(base);
    }

    /** Returns the stream itself if it supports mark/reset, otherwise a buffered wrapper. */
    private static InputStream markSupporting(final InputStream inputStream) {
        return inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
    }

    private static boolean messageStartsWith(final Exception e, final String prefix) {
        final String message = e.getMessage();
        return message != null && message.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
