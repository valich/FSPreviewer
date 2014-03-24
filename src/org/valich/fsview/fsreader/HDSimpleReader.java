package org.valich.fsview.fsreader;

import org.valich.fsview.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by valich on 20.03.14.
 */
final class HDSimpleReader extends AbstractSimpleFSReader {
    HDSimpleReader() {
        super(FileSystems.getDefault());
    }

    @Override public boolean changeDirectory(Path path) {
        if (path == null || !Files.isDirectory(getPath().resolve(path)))
            return false;

        return super.changeDirectory(path);
    }

    @Override
    public Collection<? extends FileInfo> getDirectoryContents() {
        assert Files.isDirectory(getPath()) : "Current directory is not directory " + getPath();

        Collection<FileInfo> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getPath())) {
            for (Path entry : stream) {
                result.add(FileInfo.valueOf(entry));
            }
        } catch (IOException e) {
//            e.printStackTrace();
            Logger.getLogger("test").warning("Could not open dir: " + getPath());
            return result;
        }

        return result;
    }

    @Override
    public FileInfo getFileByPath(Path path) {
        return FileInfo.valueOf(getPath().resolve(path));
    }

    @Override
    public InputStream retrieveFileInputStream(Path path) throws IOException {
        if (!Files.isRegularFile(getPath().resolve(path)))
            return null;

        return Files.newInputStream(getPath().resolve(path));
    }
}
