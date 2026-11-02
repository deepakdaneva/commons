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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ArchiveUtil}.
 *
 * @author Deepak Kumar Jangir
 */
class TestArchiveUtil {

    /** Content of the {@code txt_file} entry inside every fixture archive. */
    static final byte[] HELLO = "Hello World".getBytes(StandardCharsets.US_ASCII);

    static final String ENTRY_NAME = "txt_file";

    static InputStream resource(final String name) {
        final InputStream in = TestArchiveUtil.class.getResourceAsStream("/testfiles/" + name);
        assertNotNull(in, "missing test resource " + name);
        return new BufferedInputStream(in);
    }

    /** Asserts that the archive holds exactly one entry named {@code txt_file} containing {@code Hello World}. */
    @SuppressWarnings("rawtypes")
    static void assertSingleHelloEntry(final ArchiveInputStream ais) throws IOException {
        assertNotNull(ais);
        final ArchiveEntry entry = ais.getNextEntry();
        assertNotNull(entry);
        assertEquals(ENTRY_NAME, entry.getName());
        assertArrayEquals(HELLO, ArchiveUtil.getEntryInputStream(ais).readAllBytes());
        assertNull(ais.getNextEntry());
    }

    static byte[] tar(final byte[] content) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            final TarArchiveEntry entry = new TarArchiveEntry(ENTRY_NAME);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

    static byte[] gzip(final byte[] content) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream out = new GzipCompressorOutputStream(bytes)) {
            out.write(content);
        }
        return bytes.toByteArray();
    }

    static byte[] bzip2(final byte[] content) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream out = new BZip2CompressorOutputStream(bytes)) {
            out.write(content);
        }
        return bytes.toByteArray();
    }

    /** A stream that does not support mark/reset, to check that the utility wraps it. */
    static InputStream nonMarkable(final byte[] content) {
        return new FilterInputStream(new ByteArrayInputStream(content)) {
            @Override
            public boolean markSupported() {
                return false;
            }
        };
    }

    @Nested
    class MimeTypes {

        @ParameterizedTest
        @ValueSource(strings = {"application/zip", "application/java-archive", "application/x-tar", "application/x-gtar", "application/x-arj", "application/x-archive", "application/x-cpio", "application/x-tika-unix-dump", "application/x-7z-compressed", "application/x-rar-compressed"})
        void archiveMimeTypes(final String mimeType) {
            assertTrue(ArchiveUtil.isArchiveMimeType(mimeType));
            assertTrue(ArchiveUtil.isAnyArchiveMimeType(mimeType));
            assertFalse(ArchiveUtil.isCompressorMimeType(mimeType));
        }

        @Test
        void compressorMimeTypes() {
            assertFalse(ArchiveUtil.COMPRESSOR_MIME_TYPES.isEmpty());
            for (final String mimeType : ArchiveUtil.COMPRESSOR_MIME_TYPES) {
                assertTrue(ArchiveUtil.isCompressorMimeType(mimeType), mimeType);
                assertTrue(ArchiveUtil.isAnyArchiveMimeType(mimeType), mimeType);
                assertFalse(ArchiveUtil.isArchiveMimeType(mimeType), mimeType);
            }
        }

        @Test
        void compressorMimeTypesAreImmutable() {
            assertThrows(UnsupportedOperationException.class, () -> ArchiveUtil.COMPRESSOR_MIME_TYPES.add("x/y"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/zip; charset=UTF-8", "application/zip;", "APPLICATION/ZIP", " application/zip "})
        void parametersCaseAndWhitespaceAreIgnored(final String mimeType) {
            assertTrue(ArchiveUtil.isArchiveMimeType(mimeType));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {";", " ", "text/plain", "application/pdf", "application/zipx", "zip"})
        void unknownMimeTypes(final String mimeType) {
            assertFalse(ArchiveUtil.isAnyArchiveMimeType(mimeType));
            assertFalse(ArchiveUtil.isArchiveMimeType(mimeType));
            assertFalse(ArchiveUtil.isCompressorMimeType(mimeType));
            assertFalse(ArchiveUtil.isRarMimeType(mimeType));
            assertFalse(ArchiveUtil.is7zMimeType(mimeType));
        }

        @Test
        void rarAnd7z() {
            assertTrue(ArchiveUtil.isRarMimeType("application/x-rar-compressed"));
            assertFalse(ArchiveUtil.isRarMimeType("application/x-7z-compressed"));
            assertTrue(ArchiveUtil.is7zMimeType("application/x-7z-compressed; foo=bar"));
            assertFalse(ArchiveUtil.is7zMimeType("application/zip"));
        }

        @Test
        void brotliOrLzmaName() {
            assertEquals(CompressorStreamFactory.getBrotli(), ArchiveUtil.getBrotliOrLzmaName("application/x-brotli"));
            assertEquals(CompressorStreamFactory.getLzma(), ArchiveUtil.getBrotliOrLzmaName("application/x-lzma"));
            assertEquals(CompressorStreamFactory.getLzma(), ArchiveUtil.getBrotliOrLzmaName("Application/X-LZMA"));
            assertNull(ArchiveUtil.getBrotliOrLzmaName("application/gzip"));
            assertNull(ArchiveUtil.getBrotliOrLzmaName(null));
        }

        @Test
        void semicolonConstant() {
            assertEquals(";", ArchiveUtil.SEMICOLON);
        }
    }

    @Nested
    class GetArchiveInputStream {

        @Test
        void zip() throws Exception {
            try (InputStream in = resource("txt_file_zip"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/zip")) {
                assertInstanceOf(ZipArchiveInputStream.class, ais);
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void zipWithMimeParameters() throws Exception {
            try (InputStream in = resource("txt_file_zip"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/zip; charset=binary")) {
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void zipFromStreamWithoutMarkSupport() throws Exception {
            final byte[] zip;
            try (InputStream in = resource("txt_file_zip")) {
                zip = in.readAllBytes();
            }
            try (ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(nonMarkable(zip), "application/zip")) {
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void tarGz() throws Exception {
            try (InputStream in = resource("txt_file_tar_gz"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/gzip")) {
                assertInstanceOf(TarArchiveInputStream.class, ais);
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void plainTar() throws Exception {
            try (ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(new ByteArrayInputStream(tar(HELLO)), "application/x-tar")) {
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void tarBzip2FromStreamWithoutMarkSupport() throws Exception {
            try (ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(nonMarkable(bzip2(tar(HELLO))), "application/x-bzip2")) {
                assertSingleHelloEntry(ais);
            }
        }

        @Test
        void sevenZ() throws Exception {
            try (InputStream in = resource("txt_file_7z"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/x-7z-compressed")) {
                assertInstanceOf(SevenZArchiveInputStream.class, ais);
                assertSingleHelloEntry(ais);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/zip", "application/x-tar", "application/x-7z-compressed", "application/gzip", "application/x-bzip2"})
        void plainTextIsNotAnArchive(final String mimeType) throws IOException {
            try (InputStream in = resource("txt_file")) {
                assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getArchiveInputStream(in, mimeType));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/zip", "application/x-7z-compressed", "application/gzip"})
        void emptyInputIsNotAnArchive(final String mimeType) {
            final InputStream empty = new ByteArrayInputStream(new byte[0]);
            assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getArchiveInputStream(empty, mimeType));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"application/x-rar-compressed", "text/plain", "application/octet-stream"})
        void unsupportedMimeType(final String mimeType) throws IOException {
            try (InputStream in = resource("txt_file_zip")) {
                final NotAnArchiveOrSupportedArchiveException e = assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getArchiveInputStream(in, mimeType));
                assertTrue(e.getMessage().startsWith("Not an archive or an unsupported archive file."));
            }
        }

        @Test
        void nullStream() {
            assertThrows(NullPointerException.class, () -> ArchiveUtil.getArchiveInputStream(null, "application/zip"));
        }

        @Test
        void sevenZWithEncryptedHeaders() throws IOException {
            try (InputStream in = resource("txt_file_7z_password_headers")) {
                final PasswordProtectedArchiveException e = assertThrows(PasswordProtectedArchiveException.class, () -> ArchiveUtil.getArchiveInputStream(in, "application/x-7z-compressed"));
                assertInstanceOf(PasswordRequiredException.class, e.getCause());
            }
        }

        @Test
        void ignoreExceptionReturnsNull() throws Exception {
            try (InputStream in = resource("txt_file")) {
                assertNull(ArchiveUtil.getArchiveInputStream(in, "application/zip", true));
            }
            assertNull(ArchiveUtil.getArchiveInputStream(null, "application/zip", true));
            assertNull(ArchiveUtil.getArchiveInputStream(new ByteArrayInputStream(HELLO), "text/plain", true));
        }

        @Test
        void ignoreExceptionFalsePropagates() throws IOException {
            try (InputStream in = resource("txt_file")) {
                assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getArchiveInputStream(in, "application/zip", false));
            }
        }

        @Test
        void ignoreExceptionStillReturnsStream() throws Exception {
            try (InputStream in = resource("txt_file_zip"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/zip", true)) {
                assertSingleHelloEntry(ais);
            }
        }
    }

    @Nested
    class GetCompressorInputStream {

        @Test
        void autoDetectGzip() throws Exception {
            try (CompressorInputStream in = ArchiveUtil.getCompressorInputStream(new ByteArrayInputStream(gzip(HELLO)), null)) {
                assertArrayEquals(HELLO, in.readAllBytes());
            }
        }

        @Test
        void autoDetectFromStreamWithoutMarkSupport() throws Exception {
            try (CompressorInputStream in = ArchiveUtil.getCompressorInputStream(nonMarkable(bzip2(HELLO)), null)) {
                assertArrayEquals(HELLO, in.readAllBytes());
            }
        }

        @Test
        void explicitName() throws Exception {
            try (CompressorInputStream in = ArchiveUtil.getCompressorInputStream(new ByteArrayInputStream(gzip(HELLO)), CompressorStreamFactory.GZIP)) {
                assertArrayEquals(HELLO, in.readAllBytes());
            }
        }

        @Test
        void notCompressed() {
            assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getCompressorInputStream(new ByteArrayInputStream(HELLO), null));
            assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> ArchiveUtil.getCompressorInputStream(new ByteArrayInputStream(new byte[0]), null));
        }

        @Test
        void unknownCompressorName() {
            assertThrows(CompressorException.class, () -> ArchiveUtil.getCompressorInputStream(new ByteArrayInputStream(HELLO), "no-such-compressor"));
        }

        @Test
        void nullStream() {
            assertThrows(NullPointerException.class, () -> ArchiveUtil.getCompressorInputStream(null, null));
        }
    }

    @Nested
    class GetEntryInputStream {

        @Test
        void returnsBufferedCopy() throws Exception {
            final InputStream copy = ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(HELLO));
            assertInstanceOf(BufferedInputStream.class, copy);
            assertArrayEquals(HELLO, copy.readAllBytes());
        }

        @Test
        void emptyEntry() throws Exception {
            assertEquals(0, ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(new byte[0])).readAllBytes().length);
        }

        @Test
        void sizeLimit() throws Exception {
            assertArrayEquals(HELLO, ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(HELLO), HELLO.length).readAllBytes());
            assertThrows(IOException.class, () -> ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(HELLO), HELLO.length - 1));
            assertEquals(0, ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(new byte[0]), 0).readAllBytes().length);
        }

        @Test
        void invalidArguments() {
            assertThrows(IllegalArgumentException.class, () -> ArchiveUtil.getEntryInputStream(new ByteArrayInputStream(HELLO), -1));
            assertThrows(NullPointerException.class, () -> ArchiveUtil.getEntryInputStream(null));
        }

        @Test
        void encryptedZipEntry() throws Exception {
            try (InputStream in = resource("txt_file_zip_password"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/zip")) {
                assertNotNull(ais.getNextEntry());
                final PasswordProtectedArchiveException e = assertThrows(PasswordProtectedArchiveException.class, () -> ArchiveUtil.getEntryInputStream(ais));
                assertInstanceOf(UnsupportedZipFeatureException.class, e.getCause());
            }
        }

        @Test
        void encrypted7zEntry() throws Exception {
            // 7z decoders are set up eagerly, so an encrypted entry is already detected when advancing to it.
            try (InputStream in = resource("txt_file_7z_password"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/x-7z-compressed")) {
                assertThrows(PasswordRequiredException.class, ais::getNextEntry);
            }
        }

        @Test
        void unsupportedZipCompressionMethodIsReported() throws Exception {
            try (InputStream in = resource("txt_file_zip_ppmd"); ArchiveInputStream<?> ais = ArchiveUtil.getArchiveInputStream(in, "application/zip")) {
                assertNotNull(ais.getNextEntry());
                final UnsupportedZipFeatureException e = assertThrows(UnsupportedZipFeatureException.class, () -> ArchiveUtil.getEntryInputStream(ais));
                assertEquals(UnsupportedZipFeatureException.Feature.METHOD, e.getFeature());
            }
        }
    }

    @Nested
    class CloseQuietly {

        /** Archive stream that records whether it was closed and always fails to close. */
        final class FailingCloseStream extends ArchiveInputStream<ArchiveEntry> {
            boolean closed;

            @Override
            public ArchiveEntry getNextEntry() {
                return null;
            }

            @Override
            public void close() throws IOException {
                closed = true;
                throw new IOException("close failed");
            }
        }

        @Test
        @SuppressWarnings("rawtypes")
        void closesAllAndIgnoresFailuresAndNulls() {
            final FailingCloseStream first = new FailingCloseStream();
            final FailingCloseStream second = new FailingCloseStream();
            final List<ArchiveInputStream> streams = new ArrayList<>(Arrays.asList(first, null, second));
            ArchiveUtil.closeQuietly(streams);
            assertTrue(first.closed);
            assertTrue(second.closed);
        }

        @Test
        void nullAndEmptyList() {
            ArchiveUtil.closeQuietly(null);
            ArchiveUtil.closeQuietly(new ArrayList<>());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void constructorIsKeptForCompatibility() {
        assertNotNull(new ArchiveUtil());
    }

    @Test
    void exceptionsKeepMessageAndCause() {
        final Throwable cause = new IOException("cause");
        assertEquals("msg", new NotAnArchiveOrSupportedArchiveException("msg").getMessage());
        assertSame(cause, new NotAnArchiveOrSupportedArchiveException("msg", cause).getCause());
        final PasswordProtectedArchiveException e = new PasswordProtectedArchiveException("msg", cause);
        assertEquals("msg", e.getMessage());
        assertSame(cause, e.getCause());
    }
}
