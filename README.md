# Liferay Zip NIO Optimization (Reader & Writer)

This module provides a high-performance implementation for both **ZipReader** and **ZipWriter** Liferay interfaces. It is specifically engineered to handle large **Liferay Archive (LAR)** files (multi-GB) containing thousands of XML entries, drastically optimizing CPU usage and Java Heap management during both Import and Export processes.

---

## 🚀 Problem vs. Solution

### Legacy Implementation (`java.util.zip` / Default Liferay)
* **ZipReader Inefficiency:** Every time an entry is requested or a folder is listed, the original implementation re-opens the file and re-scans the ZIP's Central Directory.
* **ZipWriter Bottleneck:** The default writer often opens and closes the `ZipFileSystem` for every entry or batch of entries. This forces a full rewrite of the ZIP Central Directory multiple times during a single export.
* **CPU & I/O Spikes:** For a LAR with 10,000 files, the legacy approach performs thousands of redundant "mount/unmount" operations, leading to massive CPU overhead.

### Optimized NIO Implementation (`java.nio.file.ZipFileSystem`)
* **Persistent FileSystem Session:** Unlike the default implementation, our `ZipWriter` keeps the `FileSystem` open until the process is finished. The ZIP index is written **only once** at the end.
* **True Random Access (Reader):** Uses a virtual file system mapping. Access to any file within a multi-GB LAR is nearly instantaneous ($O(1)$ complexity).
* **Resource Efficiency:** Native UTF-8 support (no Reflection hacks) and optimized memory buffers reduce the pressure on the Java Garbage Collector.

---

## ✨ Key Features

* **Stateful Writing:** Keeps the ZIP stream open during the entire lifecycle of an export, eliminating the "open-write-close" loop.
* **Thread-Safe Reader:** Handles `FileSystemAlreadyExistsException` to allow safe concurrent access when Liferay validates and imports the same file simultaneously.
* **Memory Safeguards:** Features `useTempFile` configuration to offload ZIP structure management from RAM to disk.
