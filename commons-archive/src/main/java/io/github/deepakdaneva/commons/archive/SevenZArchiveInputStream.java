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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class SevenZArchiveInputStream extends ArchiveInputStream {
    private final SevenZFile sevenZFile;

    public SevenZArchiveInputStream(final InputStream inputStream) throws IOException {
        SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(IOUtils.toByteArray(inputStream));
        this.sevenZFile = new SevenZFile(inMemoryByteChannel);
    }

    public int read() throws IOException {
        return this.sevenZFile.read();
    }

    public int read(byte[] b) throws IOException {
        return this.sevenZFile.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return this.sevenZFile.read(b, off, len);
    }

    public ArchiveEntry getNextEntry() throws IOException {
        return this.sevenZFile.getNextEntry();
    }

    public void close() throws IOException {
        this.sevenZFile.close();
    }
}
