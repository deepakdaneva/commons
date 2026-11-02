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

import static io.github.deepakdaneva.commons.archive.TestArchiveUtil.ENTRY_NAME;
import static io.github.deepakdaneva.commons.archive.TestArchiveUtil.HELLO;
import static io.github.deepakdaneva.commons.archive.TestArchiveUtil.resource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SevenZArchiveInputStream}.
 *
 * @author Deepak Kumar Jangir
 */
class TestSevenZArchiveInputStream {

    private static byte[] sevenZBytes() throws IOException {
        try (InputStream in = resource("txt_file_7z")) {
            return in.readAllBytes();
        }
    }

    @Test
    void readsEntriesByteByByte() throws IOException {
        try (SevenZArchiveInputStream in = new SevenZArchiveInputStream(new ByteArrayInputStream(sevenZBytes()))) {
            final ArchiveEntry entry = in.getNextEntry();
            assertNotNull(entry);
            assertEquals(ENTRY_NAME, entry.getName());
            assertEquals(HELLO.length, entry.getSize());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            assertArrayEquals(HELLO, out.toByteArray());
            assertEquals(HELLO.length, in.getBytesRead());
            assertNull(in.getNextEntry());
        }
    }

    @Test
    void readsIntoArray() throws IOException {
        try (SevenZArchiveInputStream in = new SevenZArchiveInputStream(new ByteArrayInputStream(sevenZBytes()))) {
            assertNotNull(in.getNextEntry());
            final byte[] buffer = new byte[64];
            final int n = in.read(buffer);
            assertEquals(HELLO.length, n);
            assertEquals(-1, in.read(buffer, 0, buffer.length));
        }
    }

    @Test
    void skipReadsThroughEntry() throws IOException {
        try (SevenZArchiveInputStream in = new SevenZArchiveInputStream(new ByteArrayInputStream(sevenZBytes()))) {
            assertNotNull(in.getNextEntry());
            assertEquals(0, in.skip(0));
            assertEquals(6, in.skip(6));
            assertArrayEquals("World".getBytes(java.nio.charset.StandardCharsets.US_ASCII), in.readAllBytes());
            assertEquals(0, in.skip(10));
        }
    }

    @Test
    void sizeLimit() throws IOException {
        final byte[] bytes = sevenZBytes();
        try (SevenZArchiveInputStream in = new SevenZArchiveInputStream(new ByteArrayInputStream(bytes), bytes.length)) {
            assertNotNull(in.getNextEntry());
        }
        assertThrows(IOException.class, () -> new SevenZArchiveInputStream(new ByteArrayInputStream(bytes), bytes.length - 1));
        assertThrows(IllegalArgumentException.class, () -> new SevenZArchiveInputStream(new ByteArrayInputStream(bytes), -1));
    }

    @Test
    void invalidInput() {
        assertThrows(NullPointerException.class, () -> new SevenZArchiveInputStream(null));
        assertThrows(IOException.class, () -> new SevenZArchiveInputStream(new ByteArrayInputStream(HELLO)));
        assertThrows(IOException.class, () -> new SevenZArchiveInputStream(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void encryptedHeaders() throws IOException {
        try (InputStream in = resource("txt_file_7z_password_headers")) {
            assertThrows(PasswordRequiredException.class, () -> new SevenZArchiveInputStream(in));
        }
    }

    @Test
    void closeIsIdempotent() throws IOException {
        final SevenZArchiveInputStream in = new SevenZArchiveInputStream(new ByteArrayInputStream(sevenZBytes()));
        in.close();
        in.close();
    }
}
