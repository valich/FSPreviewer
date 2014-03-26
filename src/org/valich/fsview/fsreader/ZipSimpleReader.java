package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

final class ZipSimpleReader extends AbstractSimpleFSReader {
    ZipSimpleReader(Path pathToArchive) throws IOException {
        super(FileSystems.newFileSystem(pathToArchive, null));
    }

    @Override public boolean changeDirectory(@NotNull Path path) throws IOException {
        if (!Files.isDirectory(getPath().resolve(path)))
            return false;

        return super.changeDirectory(path);
    }

    @NotNull
    @Override
    public Collection<FileInfo> getDirectoryContents() throws  IOException {
        assert Files.isDirectory(getPath()) : "Current directory is not directory";

        Collection<FileInfo> result = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(getPath());
        for (Path entry : stream) {
            result.add(FileInfo.valueOf(entry));
        }
        stream.close();

        return result;
    }

    @NotNull
    @Override
    public FileInfo getFileByPath(@NotNull Path path) throws IOException {
        return FileInfo.valueOf(getPath().resolve(path));
    }

    @NotNull
    @Override
    public InputStream retrieveFileInputStream(@NotNull Path path) throws IOException {
        if (!Files.isRegularFile(getPath().resolve(path)))
            throw new FileNotFoundException("Not a file");

        return Files.newInputStream(getPath().resolve(path));
    }
}
