package zip.nio;

import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactory;

import java.io.File;

import org.osgi.service.component.annotations.Component;

@Component(
    immediate = true,
    property = "service.ranking:Integer=1000",
    service = ZipWriterFactory.class
)
public class ZipWriterNioFactoryImpl implements ZipWriterFactory {
    @Override
    public ZipWriter getZipWriter() {
        return new ZipWriterNioImpl(); // Usará el constructor de temporal
    }

    @Override
    public ZipWriter getZipWriter(File file) {
        return new ZipWriterNioImpl(file);
    }
}