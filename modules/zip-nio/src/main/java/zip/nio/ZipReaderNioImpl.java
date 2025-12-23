package zip.nio;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.zip.ZipReader;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZipReaderNioImpl implements ZipReader {

    public ZipReaderNioImpl(File file) {
        _init(file);
    }

    public ZipReaderNioImpl(InputStream inputStream) throws IOException {
        _tempFile = FileUtil.createTempFile(inputStream);
        _init(_tempFile);
    }

    @Override
    public void close() {
        try {
            // Cerramos el FileSystem para liberar los descriptores de archivos
            if (_zipFs != null && _zipFs.isOpen()) {
                _zipFs.close();
            }
        } catch (UnsupportedOperationException e) {
            _log.debug("El FileSystem es gestionado externamente o no permite cierre manual.");
        } catch (IOException ioException) {
            _log.error("Error al cerrar el sistema de archivos del ZIP", ioException);
        } finally {
            if (_tempFile != null) {
                _tempFile.delete();
            }
        }
    }

    @Override
    public List<String> getEntries() {
        try (Stream<Path> walk = Files.walk(_root)) {
            return walk.filter(Files::isRegularFile)
                .map(this::_getRelativePath)
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] getEntryAsByteArray(String name) {
        if (Validator.isNull(name)) return null;

        Path path = _resolvePath(name);
        try {
            // En lugar de StreamUtil o ByteArrayOutputStream manual, 
            // Files.readAllBytes es más directo y a veces usa buffers internos optimizados
            if (Files.exists(path) && !Files.isDirectory(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            _log.error("Error leyendo entrada: " + name, e);
        }
        return null;
    }

    @Override
    public InputStream getEntryAsInputStream(String name) {
        if (Validator.isNull(name)) return null;

        Path path = _resolvePath(name);
        try {
            if (Files.exists(path) && !Files.isDirectory(path)) {
                return Files.newInputStream(path);
            }
        } catch (IOException e) {
            _log.error("Error obteniendo InputStream para la entrada: " + name, e);
        }
        return null;
    }

    @Override
    public String getEntryAsString(String name) {
        byte[] bytes = getEntryAsByteArray(name);
        return (bytes != null) ? new String(bytes) : null;
    }

    @Override
    public List<String> getFolderEntries(String path) {
        if (Validator.isNull(path)) return Collections.emptyList();

        Path folderPath = _resolvePath(path);
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            return Collections.emptyList();
        }

        try (Stream<Path> list = Files.list(folderPath)) {
            return list.filter(Files::isRegularFile)
                .map(this::_getRelativePath)
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void _init(File file) {
        try {
            URI uri = URI.create("jar:" + file.toURI().toString());
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");

            try {
                // Intentamos crear una nueva instancia del FileSystem
                _zipFs = FileSystems.newFileSystem(uri, env);
            } catch (FileSystemAlreadyExistsException e) {
                // Si ya existe (concurrencia en Liferay), recuperamos la existente
                _zipFs = FileSystems.getFileSystem(uri);
            }

            _root = _zipFs.getPath("/");
        } catch (IOException e) {
            throw new UncheckedIOException("Fallo al inicializar ZipNioReader para: " + file.getPath(), e);
        }
    }

    private Path _resolvePath(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return _root.resolve(name);
    }

    private String _getRelativePath(Path path) {
        String s = path.toString();
        if (s.startsWith("/")) {
            return s.substring(1);
        }
        return s;
    }

    private static final Log _log = LogFactoryUtil.getLog(ZipReaderNioImpl.class);

    private FileSystem _zipFs;
    private Path _root;
    private File _tempFile;
}