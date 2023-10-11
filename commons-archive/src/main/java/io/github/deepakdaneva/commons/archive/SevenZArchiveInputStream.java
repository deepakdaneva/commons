package io.github.deepakdaneva.commons.archive;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

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
