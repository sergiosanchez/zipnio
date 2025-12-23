package zip.nio;

import zip.nio.ZipReaderNioImpl;
import com.liferay.portal.kernel.zip.ZipReader;
import com.liferay.portal.kernel.zip.ZipReaderFactory;
import org.osgi.service.component.annotations.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component(
    immediate = true,
    property = {
        "service.ranking:Integer=1000" // Ranking muy alto para asegurar prioridad
    },
    service = ZipReaderFactory.class
)
public class ZipReaderNioFactoryImpl implements ZipReaderFactory {

    @Override
    public ZipReader getZipReader(File file) {
        // Aquí decidimos: si es directorio, podríamos usar otra impl,
        // pero para el LAR de GBs usamos la de NIO.
        return new ZipReaderNioImpl(file);
    }

    @Override
    public ZipReader getZipReader(InputStream inputStream) throws IOException {
        // NIO.2 necesita un FileSystem, así que seguimos necesitando 
        // el archivo temporal que creaba Liferay.
        return new ZipReaderNioImpl(inputStream);
    }
}