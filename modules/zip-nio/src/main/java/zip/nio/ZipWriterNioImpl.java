package zip.nio;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.SystemProperties;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.zip.ZipWriter;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ZipWriterNioImpl implements ZipWriter, AutoCloseable {

    // Constructor para archivos temporales (requerido por la Factory)
    public ZipWriterNioImpl() {
        this(new File(
            SystemProperties.get(SystemProperties.TMP_DIR) + 
            File.separator + PortalUUIDUtil.generate() + ".zip"
        ));
    }

    public ZipWriterNioImpl(File file) {
        _file = file.getAbsoluteFile();
        // Usamos toURI para evitar problemas con espacios o caracteres especiales
        _uri = URI.create("jar:" + _file.toURI().toString());
        _initFileSystem();
    }

    private void _initFileSystem() {
        try {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            env.put("encoding", "UTF-8"); 
            _zipFs = FileSystems.newFileSystem(_uri, env);
        } catch (IOException e) {
            throw new UncheckedIOException("Error initializing ZipWriterNio", e);
        }
    }

    @Override
    public void addEntry(String name, byte[] bytes) throws IOException {
        if (bytes == null) return;
        Path path = _getPath(name);
        _createParentDirectories(path);
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    @Override
    public void addEntry(String name, InputStream inputStream) throws IOException {
        if (inputStream == null) return;
        Path path = _getPath(name);
        _createParentDirectories(path);
        try (inputStream) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void addEntry(String name, String s) throws IOException {
        if (s == null) return;
        // Sustituimos StringPool.UTF8 por StandardCharsets.UTF_8
        addEntry(name, s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void addEntry(String name, StringBuilder sb) throws IOException {
        if (sb == null) return;
        addEntry(name, sb.toString());
    }

    @Override
    public File getFile() {
        try {
            close();
        } catch (IOException e) {
            _log.error("Error closing filesystem in getFile", e);
        }
        return _file;
    }

    @Override
    public void close() throws IOException {
        if (_zipFs != null && _zipFs.isOpen()) {
            _zipFs.close();
        }
    }

    @Deprecated
    @Override
    public byte[] finish() throws IOException {
        close();
        return Files.readAllBytes(_file.toPath());
    }

    @Deprecated
    @Override
    public String getPath() {
        return _file.getPath();
    }

    @Deprecated
    @Override
    public void umount() {
        try { close(); } catch (IOException e) { _log.error(e); }
    }

    private Path _getPath(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return _zipFs.getPath(name);
    }

    private void _createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static final Log _log = LogFactoryUtil.getLog(ZipWriterNioImpl.class);
    private FileSystem _zipFs;
    private final File _file;
    private final URI _uri;
}