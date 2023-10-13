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
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class ArchiveUtil {

    /** Split Delimiter for mime type */
    public static final String SEMICOLON = ";";
    /** List of COMPRESSOR FILE Mime-Types. */
    public static final Set<String> COMPRESSOR_MIME_TYPES = Set.of("application/zlib", "application/x-gzip", "application/x-bzip2", "application/x-compress", "application/x-java-pack200", "application/x-lzma", "application/deflate64", "application/x-lz4", "application/x-snappy", "application/x-brotli", "application/gzip", "application/x-bzip", "application/x-xz");
    /** 7z File Mime-Type. */
    private static final String SEVENZ_MIME_TYPE = "application/x-7z-compressed";
    /** RAR File Mime-Type. */
    private static final String RAR_MIME_TYPE = "application/x-rar-compressed";
    /** List of ARCHIVE File Mime-Types. */
    private static final Set<String> ARCHIVE_MIME_TYPES = Set.of("application/x-tar", "application/x-gtar", "application/java-archive", "application/x-arj", "application/x-archive", "application/zip", "application/x-cpio", "application/x-tika-unix-dump", SEVENZ_MIME_TYPE, RAR_MIME_TYPE);

    /**
     * This method validates whether the provided mime type is an archive/compressed/rar mime type
     * or not.
     *
     * @param mimeType which needs to be validated.
     * @return true if provided mime-type is an archive or compressed or rar file mime type else
     * false.
     */
    public static boolean isAnyArchiveMimeType(final String mimeType) {
        return isArchiveMimeType(mimeType) || isCompressorMimeType(mimeType) || isRarMimeType(mimeType);
    }

    /**
     * This method validates whether the provided mime type is an compressed mime type or not.
     * {@link ArchiveUtil#ARCHIVE_MIME_TYPES}
     *
     * @param mimeType which needs to be validated.
     * @return true if provided mime-type is an archive file mime type else false.
     */
    public static boolean isArchiveMimeType(final String mimeType) {
        return ARCHIVE_MIME_TYPES.contains(Objects.nonNull(mimeType) ? mimeType.split(SEMICOLON)[0] : null);
    }

    /**
     * This method validates whether the provided mime type is an compressed mime type or not.
     * {@link ArchiveUtil#COMPRESSOR_MIME_TYPES}
     *
     * @param mimeType which needs to be validated.
     * @return true if provided mime-type is an compressed file mime type else false.
     */
    public static boolean isCompressorMimeType(final String mimeType) {
        return COMPRESSOR_MIME_TYPES.contains(Objects.nonNull(mimeType) ? mimeType.split(SEMICOLON)[0] : null);
    }

    /**
     * This method validates whether the provided mime type is an rar mime type or not. {@link
     * ArchiveUtil#RAR_MIME_TYPE}
     *
     * @param mimeType which needs to be validated.
     * @return true if provided mime-type is a rar file mime type else false.
     */
    public static boolean isRarMimeType(final String mimeType) {
        return RAR_MIME_TYPE.equals(Objects.nonNull(mimeType) ? mimeType.split(SEMICOLON)[0] : null);
    }

    /**
     * This method validates whether the provided mime type is an 7z mime type or not. {@link
     * ArchiveUtil#SEVENZ_MIME_TYPE}
     *
     * @param mimeType which needs to be validated.
     * @return true if provided mime-type is a 7z file mime type else false.
     */
    public static boolean is7zMimeType(final String mimeType) {
        return SEVENZ_MIME_TYPE.equals(Objects.nonNull(mimeType) ? mimeType.split(SEMICOLON)[0] : null);
    }

    /**
     * This method return the actual name which can be used by Apache Commons Compress library to
     * create actual brotli/lzma CompressorInputStream.
     *
     * @param mimeType string to check if it contains {@code brotli} or {@code lzma} sub string.
     * @return null or {@link CompressorStreamFactory#BROTLI}/{@link CompressorStreamFactory#LZMA}.
     */
    public static String getBrotliOrLzmaName(final String mimeType) {
        if (Objects.isNull(mimeType)) {
            return null;
        } else if (mimeType.contains("brotli")) {
            return CompressorStreamFactory.getBrotli();
        } else if (mimeType.contains("lzma")) {
            return CompressorStreamFactory.getLzma();
        } else {
            return null;
        }
    }

    /**
     * This method provides the {@link ArchiveInputStream} of the InputStream.
     *
     * @param inputStream InputStream of the archive file which supports mark and reset, recommended
     * BufferedInputStream.
     * @param mimeType of the inputStream, which will be used create proper archive input stream.
     * @return {@link ArchiveInputStream} instance to work with the archive file.
     * @throws CompressorException is any compressor exception is raised.
     * @throws IOException if any IOException is raised.
     * @throws ArchiveException if any ArchiveException is raised.
     * @throws NotAnArchiveOrSupportedArchiveException RuntimeException if provided inputStream is
     * not an archive.
     * @throws PasswordProtectedArchiveException RuntimeException if any password protected archive
     * exception raised.
     */
    public static ArchiveInputStream getArchiveInputStream(final InputStream inputStream, final String mimeType) throws ArchiveException, IOException, CompressorException, NotAnArchiveOrSupportedArchiveException {
        if (is7zMimeType(mimeType)) {
            try {
                return new SevenZArchiveInputStream(inputStream);
            } catch (PasswordRequiredException e) {
                throw new PasswordProtectedArchiveException("Password protected archive/entries not supported", e);
            } catch (EOFException eofe) {
                throw new NotAnArchiveOrSupportedArchiveException("Not an archive or an unsupported archive file.", eofe);
            } catch (IOException ioe) {
                if ("Bad 7z signature".equalsIgnoreCase(ioe.getMessage())) {
                    throw new NotAnArchiveOrSupportedArchiveException("Not an archive or an unsupported archive file.", ioe);
                } else {
                    throw ioe;
                }
            }
        } else if (!isRarMimeType(mimeType) && isArchiveMimeType(mimeType)) {
            try {
                return ArchiveStreamFactory.DEFAULT.createArchiveInputStream(inputStream);
            } catch (ArchiveException ae) {
                if ("No Archiver found for the stream signature".equalsIgnoreCase(ae.getMessage())) {
                    throw new NotAnArchiveOrSupportedArchiveException("Not an archive or an unsupported archive file.", ae);
                } else {
                    throw ae;
                }
            }
        } else if (isCompressorMimeType(mimeType)) {
            return new TarArchiveInputStream(getCompressorInputStream(inputStream, getBrotliOrLzmaName(mimeType)));
        }
        throw new NotAnArchiveOrSupportedArchiveException("Not an archive or an unsupported archive file.");
    }

    /**
     * This method provides the {@link CompressorInputStream} of the InputStream.
     *
     * @param inputStream InputStream of the archive file which supports mark and reset, recommended
     * BufferedInputStream.
     * @param archiverName which is optional and will only be used in case no CompressorInputStream
     * are created without using it.
     * @return {@link CompressorInputStream} instance to work with the archive file or null.
     * @throws NotAnArchiveOrSupportedArchiveException RuntimeException if provided inputStream is
     * not an archive.
     * @throws CompressorException if any compressor exception is raised.
     */
    public static CompressorInputStream getCompressorInputStream(final InputStream inputStream, final String archiverName) throws CompressorException {
        try {
            if (Objects.nonNull(archiverName)) {
                return CompressorStreamFactory.getSingleton().createCompressorInputStream(archiverName, inputStream);
            } else {
                return CompressorStreamFactory.getSingleton().createCompressorInputStream(inputStream);
            }
        } catch (CompressorException ce) {
            if ("No Compressor found for the stream signature.".equalsIgnoreCase(ce.getMessage())) {
                throw new NotAnArchiveOrSupportedArchiveException("Not an archive or an unsupported archive file.", ce);
            } else {
                throw ce;
            }
        }
    }

    /**
     * This method provides the ArchiveInputStream of the InputStream.
     *
     * @param inputStream InputStream of the archive file which supports mark and reset, recommended
     * BufferedInputStream.
     * @param mimeType of the inputStream, which will be used create proper archive input stream.
     * @param ignoreException if true will return null in case if any exception is raised, else will
     * throw exception.
     * @return ArchiveInputStream instance to work with the archive file or null.
     * @throws CompressorException is any compressor exception is raised.
     * @throws IOException if any IOException is raised.
     * @throws ArchiveException if any ArchiveException is raised.
     * @throws NotAnArchiveOrSupportedArchiveException RuntimeException if provided inputStream is
     * not an archive.
     * @throws PasswordProtectedArchiveException RuntimeException if any password protected archive
     * exception raised.
     */
    public static ArchiveInputStream getArchiveInputStream(final InputStream inputStream, final String mimeType, final boolean ignoreException) throws CompressorException, IOException, ArchiveException {
        try {
            return getArchiveInputStream(inputStream, mimeType);
        } catch (Exception e) {
            if (!ignoreException) throw e;
        }
        return null;
    }

    /**
     * This method creates an input stream of the current entry of the provided ArchiveInputStream.
     *
     * @param currentArcInStream which is already iterated and is at the position of the current
     * entry.
     * @return InputStream (BufferedInputStream) of the current entry of the provided
     * ArchiveInputStream or null.
     * @throws IOException in case of any exception during the creation of InputStream.
     * @throws PasswordProtectedArchiveException if password protected entry is found.
     */
    public static InputStream getEntryInputStream(final InputStream currentArcInStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(currentArcInStream, byteArrayOutputStream);
        } catch (UnsupportedZipFeatureException e) {
            if (UnsupportedZipFeatureException.Feature.ENCRYPTION.equals(e.getFeature())) {
                throw new PasswordProtectedArchiveException("Password protected archive/entries not supported", e);
            }
        }
        return new BufferedInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }

    /**
     * This method closes the list of ArchiveInputStream quietly.
     *
     * @param aisList list of ArchiveInputStream.
     */
    public static void closeQuietly(List<ArchiveInputStream> aisList) {
        if (Objects.nonNull(aisList)) {
            aisList.forEach(ais -> {
                try {
                    ais.close();
                } catch (IOException ignored) {
                }
            });
        }
    }
}
