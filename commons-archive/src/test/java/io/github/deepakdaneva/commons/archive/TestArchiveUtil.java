package io.github.deepakdaneva.commons.archive;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class TestArchiveUtil {

    @Test
    public void testZip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_zip").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/zip", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    @Test
    public void testTar() throws CompressorException, IOException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_tar_gz").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/gzip", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    @Test
    public void test7Zip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_7z").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/x-7z-compressed", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    @Test
    public void testNonZip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/zip", false);
        });
        IOUtils.closeQuietly(is);
    }

    @Test
    public void testNon7Zip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/x-7z-compressed", false);
        });
        IOUtils.closeQuietly(is);
    }

    @Test
    public void testNonTar() throws CompressorException, IOException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/gzip", false);
        });
        IOUtils.closeQuietly(is);
    }
}
