# commons

[![CI](https://github.com/deepakdaneva/commons/actions/workflows/ci.yml/badge.svg)](https://github.com/deepakdaneva/commons/actions/workflows/ci.yml)
[![CodeQL](https://github.com/deepakdaneva/commons/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/deepakdaneva/commons/actions/workflows/github-code-scanning/codeql)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.deepakdaneva.commons/commons-archive?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.deepakdaneva.commons/commons-archive)
[![javadoc](https://javadoc.io/badge2/io.github.deepakdaneva.commons/commons-archive/javadoc.svg)](https://javadoc.io/doc/io.github.deepakdaneva.commons/commons-archive)
[![GitHub release](https://img.shields.io/github/v/release/deepakdaneva/commons)](https://github.com/deepakdaneva/commons/releases/latest)
[![Java](https://img.shields.io/badge/Java-11%2B-orange)](#requirements)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-fe5196)](https://conventionalcommits.org)
[![License](https://img.shields.io/github/license/deepakdaneva/commons)](LICENSE)

A collection of small, focused Java utilities that take the boilerplate out of everyday tasks. Each module is
published separately to Maven Central, so you only depend on what you use.

## Table of contents

- [Modules](#modules)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick example](#quick-example)
- [Compatibility](#compatibility)
- [Building from source](#building-from-source)
- [Contributing](#contributing)
- [Security](#security)
- [Changelog](#changelog)
- [License and notice](#license-and-notice)

## Modules

| Module                                 | Artifact                                             | Description                                                                                                  |
|----------------------------------------|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [commons-archive](commons-archive/README.md) | `io.github.deepakdaneva.commons:commons-archive` | Detect archive / compressor MIME types and open zip, jar, tar, 7z, gzip, bzip2, xz (and more) streams through Apache Commons Compress. |

## Requirements

- Java 11 or newer (tested on 11, 17, 21 and 25).
- Module-specific runtime dependencies are declared with `provided` scope, so your application chooses their versions.
  See each module's README for details.

## Installation

The latest release is **1.1.1**. <!-- x-release-please-version -->

### Maven

```xml
<dependency>
    <groupId>io.github.deepakdaneva.commons</groupId>
    <artifactId>commons-archive</artifactId>
    <version>1.1.1</version> <!-- x-release-please-version -->
</dependency>
```

### Gradle

```groovy
implementation 'io.github.deepakdaneva.commons:commons-archive:1.1.1' // x-release-please-version
```

Remember to add the module's `provided` dependencies as well (for `commons-archive`: Apache Commons Compress and
Apache Commons IO). See [commons-archive/README.md](commons-archive/README.md#installation).

## Quick example

```java
try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of("data.zip")));
     ArchiveInputStream<?> archive = ArchiveUtil.getArchiveInputStream(in, "application/zip")) {
    ArchiveEntry entry;
    while ((entry = archive.getNextEntry()) != null) {
        System.out.println(entry.getName());
    }
}
```

Compressed single files such as `notes.txt.gz` (no TAR inside) come back as a one-entry
`CompressedFileArchiveInputStream`, so the same loop reads them too:

```java
try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of("notes.txt.gz")));
     ArchiveInputStream<?> archive = ArchiveUtil.getArchiveInputStream(in, "application/gzip")) {
    boolean singleFile = archive instanceof CompressedFileArchiveInputStream; // false for data.tar.gz
    ArchiveEntry entry = archive.getNextEntry(); // name from the gzip header, or null if not stored
    byte[] content = ArchiveUtil.getEntryInputStream(archive, 50L * 1024 * 1024).readAllBytes();
}
```

More examples are in the [commons-archive README](commons-archive/README.md#usage).

## Compatibility

| commons version | Java baseline | Tested on JDK   | Apache Commons Compress | Apache Commons IO |
|-----------------|---------------|-----------------|-------------------------|-------------------|
| 1.1.0           | 11            | 11, 17, 21, 25  | 1.26.0+ (built with 1.28.0) | 2.x (built with 2.22.0) |
| 1.0.0           | 11            | 11              | 1.24.0                  | 2.13.0            |

## Building from source

The project uses the Maven Wrapper, so no local Maven installation is needed (a JDK 11+ is).

```shell
git clone https://github.com/deepakdaneva/commons.git
cd commons

# Compile, run the tests, package the jars and check formatting
./mvnw verify

# Re-format sources (Eclipse formatter + license header) before committing
./mvnw spotless:apply
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

The build fails in the `verify` phase if a source file is not formatted; run `./mvnw spotless:apply` to fix it.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.

## Security

Please do not report security vulnerabilities through public issues. See [SECURITY.md](SECURITY.md) for how to report
them privately.

## Changelog

Release notes are kept in [CHANGELOG.md](CHANGELOG.md).

## License and notice

Licensed under the [Apache License, Version 2.0](LICENSE). Third-party attributions are listed in [NOTICE](NOTICE).

Copyright (C) 2023 Deepak Kumar Jangir
