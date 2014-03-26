package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

enum SimpleFSReaderFactory {
    INSTANCE;

    public SimpleFSReader getReaderForBasePath(String path) throws IllegalArgumentException, IOException {
        String protocol = PathHelper.getProtocol(path);

        if (protocol == null) {
            return new HDSimpleReader();
        }

        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot build URI", e);
        }

        if (uri.getScheme().equals("ftp")) {
            try {
                return new FTPSimpleReader(uri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Bad URI syntax!", e);
            }
        }

        throw new IllegalArgumentException(path + "is not supported");
    }

    public boolean isSupportedFileName(String fileName) {
        for (SupportedFiles fType : SupportedFiles.values())
            if (fType.isSupportedFileName(fileName))
                return true;

        return false;
    }

    @NotNull
    public SimpleFSReader getReaderForFile(@NotNull Path pathToFile) throws IllegalArgumentException, IOException {
        if (!Files.isRegularFile(pathToFile))
            throw new IllegalArgumentException(pathToFile.toString() + " is not file");

        String fileName = pathToFile.getFileName().toString();

        for (SupportedFiles fType : SupportedFiles.values())
            if (fType.isSupportedFileName(fileName))
                return fType.getFileReader(pathToFile);

        throw new IllegalArgumentException(fileName + " is not kinda supported");
    }

    public enum SupportedFiles {
        ZIP {
            @Override
            public boolean isSupportedFileName(String fileName) {
                return fileName.matches(".*\\.zip");
            }

            @NotNull
            @Override
            public SimpleFSReader getFileReader(Path path) throws IOException {
                return new ZipSimpleReader(path);
            }
        };

        public abstract boolean isSupportedFileName(String fileName);
        public abstract SimpleFSReader getFileReader(Path path) throws IOException;
    }
}
