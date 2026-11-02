# commons-archive

[![CI](https://github.com/deepakdaneva/commons/actions/workflows/ci.yml/badge.svg)](https://github.com/deepakdaneva/commons/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.deepakdaneva.commons/commons-archive?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.deepakdaneva.commons/commons-archive)
[![javadoc](https://javadoc.io/badge2/io.github.deepakdaneva.commons/commons-archive/javadoc.svg)](https://javadoc.io/doc/io.github.deepakdaneva.commons/commons-archive)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Utilities for detecting and reading archives and compressed streams on top of
[Apache Commons Compress](https://commons.apache.org/proper/commons-compress/). Give it a stream and its MIME type
(for example from Apache Tika or a `Content-Type` header) and it returns the right `ArchiveInputStream`, with clear
exceptions for unsupported, malformed and password-protected content.

## Features

- MIME type checks: `isAnyArchiveMimeType`, `isArchiveMimeType`, `isCompressorMimeType`, `is7zMimeType`,
  `isRarMimeType`. Matching ignores case, surrounding whitespace and parameters such as `; charset=UTF-8`.
- One entry point, `ArchiveUtil.getArchiveInputStream(InputStream, String)`, for:
  - archives: zip, jar, tar, ar, arj, cpio, dump (format detected from the stream signature);
  - 7z, via `SevenZArchiveInputStream`, a streaming `ArchiveInputStream` view of `SevenZFile`;
  - compressed tarballs: gzip, bzip2, xz, lzma, brotli and the other formats in `ArchiveUtil.COMPRESSOR_MIME_TYPES`
    that Commons Compress can read.
- `getCompressorInputStream` to decompress a single compressed stream (auto-detected or by name).
- `getEntryInputStream` to copy the current entry into memory, with an optional size limit against decompression bombs.
- Dedicated unchecked exceptions: `NotAnArchiveOrSupportedArchiveException` and `PasswordProtectedArchiveException`.
- Streams without mark/reset support are buffered automatically where format detection needs it.

RAR is recognised by `isRarMimeType` but cannot be read (Commons Compress has no RAR support).

## Requirements

- Java 11 or newer.
- Apache Commons Compress **1.26.0 or newer** and Apache Commons IO 2.x on the class path. Both are declared with
  `provided` scope, so they are not pulled in transitively: your application declares them and controls their versions.
  Use Commons Compress 1.26.0+ in any case, as older versions are affected by CVE-2024-25710 and CVE-2024-26308.
- Optional: [`org.tukaani:xz`](https://central.sonatype.com/artifact/org.tukaani/xz) to read **7z** archives and
  xz/lzma streams (Commons Compress needs it to decode 7z entries).

## Installation

The latest release is **1.1.0**. <!-- x-release-please-version -->

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.github.deepakdaneva.commons</groupId>
        <artifactId>commons-archive</artifactId>
        <version>1.1.0</version> <!-- x-release-please-version -->
    </dependency>

    <!-- provided dependencies: choose your own versions -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-compress</artifactId>
        <version>1.28.0</version>
    </dependency>
    <dependency>
        <groupId>commons-io</groupId>
        <artifactId>commons-io</artifactId>
        <version>2.22.0</version>
    </dependency>

    <!-- only needed for 7z, xz and lzma -->
    <dependency>
        <groupId>org.tukaani</groupId>
        <artifactId>xz</artifactId>
        <version>1.12</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
dependencies {
    implementation 'io.github.deepakdaneva.commons:commons-archive:1.1.0' // x-release-please-version
    implementation 'org.apache.commons:commons-compress:1.28.0'
    implementation 'commons-io:commons-io:2.22.0'
    implementation 'org.tukaani:xz:1.12' // only needed for 7z, xz and lzma
}
```

## Usage

### Check a MIME type

```java
ArchiveUtil.isAnyArchiveMimeType("application/zip");          // true
ArchiveUtil.isArchiveMimeType("Application/ZIP; charset=x");   // true
ArchiveUtil.isCompressorMimeType("application/gzip");          // true
ArchiveUtil.is7zMimeType("application/x-7z-compressed");       // true
ArchiveUtil.isRarMimeType("application/x-rar-compressed");     // true (but RAR cannot be read)
ArchiveUtil.isAnyArchiveMimeType("text/plain");                // false
```

### Iterate over the entries of an archive

```java
import io.github.deepakdaneva.commons.archive.ArchiveUtil;
import io.github.deepakdaneva.commons.archive.NotAnArchiveOrSupportedArchiveException;
import io.github.deepakdaneva.commons.archive.PasswordProtectedArchiveException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

long maxEntrySize = 50L * 1024 * 1024; // refuse entries larger than 50 MiB

try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of("upload.tar.gz")));
     ArchiveInputStream<?> archive = ArchiveUtil.getArchiveInputStream(in, "application/gzip")) {
    ArchiveEntry entry;
    while ((entry = archive.getNextEntry()) != null) {
        if (entry.isDirectory()) {
            continue;
        }
        try (InputStream content = ArchiveUtil.getEntryInputStream(archive, maxEntrySize)) {
            System.out.println(entry.getName() + ": " + content.readAllBytes().length + " bytes");
        }
    }
} catch (NotAnArchiveOrSupportedArchiveException e) {
    // not an archive, the content does not match the MIME type, or the format is not supported (e.g. RAR)
} catch (PasswordProtectedArchiveException e) {
    // the archive or one of its entries is encrypted
}
```

`getArchiveInputStream` also declares the checked `IOException`, `ArchiveException` and `CompressorException`.
Compressed MIME types such as `application/gzip` are decompressed and the result is read as a TAR archive.

### Return `null` instead of throwing

```java
ArchiveInputStream<?> archive = ArchiveUtil.getArchiveInputStream(in, mimeType, true);
if (archive == null) {
    // not a readable archive; note that `in` may have been partly consumed
}
```

### Decompress a single stream

```java
try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of("report.csv.gz")));
     CompressorInputStream csv = ArchiveUtil.getCompressorInputStream(in, null)) { // null = detect format
    // read the decompressed bytes
}

// Brotli and LZMA have no signature, so pass the compressor name explicitly:
String name = ArchiveUtil.getBrotliOrLzmaName("application/x-brotli"); // "br"
CompressorInputStream br = ArchiveUtil.getCompressorInputStream(in, name);
```

### Read a 7z archive

```java
try (SevenZArchiveInputStream sevenZ = new SevenZArchiveInputStream(in, 100L * 1024 * 1024)) { // max 100 MiB
    ArchiveEntry entry;
    while ((entry = sevenZ.getNextEntry()) != null) {
        // read the entry with sevenZ.read(...)
    }
}
```

7z needs random access, so `SevenZArchiveInputStream` buffers the whole archive in memory. Use the size-limited
constructor for untrusted input.

### Close several archive streams

```java
List<ArchiveInputStream> nested = new ArrayList<>();
// ...
ArchiveUtil.closeQuietly(nested); // ignores null elements and IOExceptions
```

## Security notes

- `getEntryInputStream(stream)` and `new SevenZArchiveInputStream(stream)` read without a size limit. For untrusted
  input use the overloads that take a maximum size.
- This library never writes entries to disk. If you do, validate every entry name against your target directory
  (resolve, normalize, and check it stays inside) to prevent path traversal ("zip slip").
- Password-protected archives are reported, not decrypted. For an encrypted 7z entry whose headers are not encrypted,
  `getNextEntry()` throws Commons Compress' `PasswordRequiredException`.

## Compatibility

| commons-archive | Java baseline | Tested on JDK  | Apache Commons Compress     | Apache Commons IO        |
|-----------------|---------------|----------------|-----------------------------|--------------------------|
| 1.1.0           | 11            | 11, 17, 21, 25 | 1.26.0+ (built with 1.28.0) | 2.x (built with 2.22.0)  |
| 1.0.0           | 11            | 11             | 1.24.0                      | 2.13.0                   |

1.1.0 is source and binary compatible with 1.0.0.

## Building from source

```shell
./mvnw verify          # compile, test, package, check formatting
./mvnw spotless:apply  # fix formatting and license headers
```

## Contributing, security and changelog

- [CONTRIBUTING.md](../CONTRIBUTING.md)
- [SECURITY.md](../SECURITY.md)
- [CHANGELOG.md](../CHANGELOG.md)

## License and notice

Licensed under the [Apache License, Version 2.0](LICENSE). Third-party attributions are listed in [NOTICE](NOTICE).
