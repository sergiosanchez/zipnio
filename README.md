# Liferay ZipReader NIO Optimization

This module provides a high-performance implementation of Liferay's `ZipReader` interface. It is specifically engineered to handle large **Liferay Archive (LAR)** files (multi-GB) containing thousands of XML entries, drastically optimizing CPU usage and Java Heap management.

## 🚀 Problem vs. Solution

### Legacy Implementation (`java.util.zip`)
* **Inefficient CPU Usage:** Every time an entry is requested or a folder is listed, the original implementation re-opens the file and re-scans the ZIP's Central Directory. This causes constant CPU spikes.
* **Sequential Scanning:** To find a specific file, it iterates through all ZIP entries, which is extremely slow for multi-gigabyte archives.
* **Disk I/O Overhead:** It fails to leverage modern Operating System memory-mapping capabilities.

### This Implementation (`java.nio.file.ZipFileSystem`)
* **True Random Access:** Uses a virtual file system mounted in native memory. Access to any file is nearly instantaneous.
* **CPU Reduction:** The index is processed only once. Calls to `getFolderEntries` access the node structure directly.
* **Heap Optimization:** Uses native buffers and features a `useTempFile` configuration to move management from RAM to disk.

## ✨ Key Features

* **Thread-Safe Initialization:** Handles `FileSystemAlreadyExistsException` for safe concurrent access in Liferay.
* **Lazy Loading:** File content is not loaded until the `InputStream` is actually requested.
* **Resource Management:** Robust `close()` implementation that releases native file descriptors and deletes temporary files.
* **OSGi Service Ranking:** Configured with `service.ranking:Integer=1000` to override the default Liferay implementation.

## 🛠️ Performance Configuration

To maximize memory savings, the implementation includes:
`env.put("useTempFile", "true");`

## 📦 Installation

1. Copy this module into `modules/` in your Liferay Workspace.
2. Deploy: `./gradlew deploy`.
3. Disable legacy factory: `scr:disable com.liferay.portal.zip.internal.reader.factory.ZipReaderFactoryImpl`

## 📊 Expected Results

* **Validation Time:** 60-80% reduction.
* **CPU Load:** Stabilization of Garbage Collector activity.
* **Reliability:** Prevention of `OutOfMemoryError` during massive imports.