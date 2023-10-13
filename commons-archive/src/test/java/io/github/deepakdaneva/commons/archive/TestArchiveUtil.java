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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class TestArchiveUtil {

    /**
     * Test {@code 'application/zip'} file
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void testZip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_zip").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/zip", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    /**
     * Test {@code 'application/gzip'} file
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void testTar() throws CompressorException, IOException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_tar_gz").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/gzip", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    /**
     * Test {@code 'application/x-7z-compressed'} file
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void test7Zip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file_7z").toString()));
        ArchiveInputStream ais = ArchiveUtil.getArchiveInputStream(is, "application/x-7z-compressed", true);
        Assertions.assertNotNull(ais);
        IOUtils.closeQuietly(ais, is);
    }

    /**
     * Test {@code 'text/plain'} file with wrong {@code 'application/zip'} mime type
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void testNonZip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/zip", false);
        });
        IOUtils.closeQuietly(is);
    }

    /**
     * Test {@code 'text/plain'} file with wrong {@code 'application/x-7z-compressed'} mime type
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void testNon7Zip() throws IOException, CompressorException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/x-7z-compressed", false);
        });
        IOUtils.closeQuietly(is);
    }

    /**
     * Test {@code 'text/plain'} file with wrong {@code 'application/gzip'} mime type
     * 
     * @throws IOException io excepion
     * @throws CompressorException compress exception
     * @throws ArchiveException archive exception
     */
    @Test
    public void testNonTar() throws CompressorException, IOException, ArchiveException {
        InputStream is = new BufferedInputStream(new FileInputStream(TestConstants.TEST_FILES_PATH.resolve("testfiles/txt_file").toString()));
        Assertions.assertThrows(NotAnArchiveOrSupportedArchiveException.class, () -> {
            ArchiveUtil.getArchiveInputStream(is, "application/gzip", false);
        });
        IOUtils.closeQuietly(is);
    }
}
