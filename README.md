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
* **OSGi Seamless Integration:** Uses high service ranking to override default Liferay components without modifying the core.

---

## 🛠️ Performance Configuration

The module is tuned with the following environment properties:
* `create: "true"` (for Writers).
* `useTempFile: "true"` (to protect Native Memory).
* `encoding: "UTF-8"` (for native filename support).

---

## 📦 Installation & Deployment

1. **Copy** this module into the `modules/` folder of your Liferay Workspace.
2. **Deploy** the bundle: `./gradlew deploy`.
3. **Crucial:** Disable the legacy factories to prevent conflicts (permanent via `osgi/configs` or temporary via Gogo Shell):

```bash
# Disable Reader Factory
scr:disable com.liferay.portal.zip.internal.reader.factory.ZipReaderFactoryImpl

# Disable Writer Factory
scr:disable com.liferay.portal.zip.internal.writer.factory.ZipWriterFactoryImpl
