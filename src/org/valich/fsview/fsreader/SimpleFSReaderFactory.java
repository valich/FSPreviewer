package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;

import javax.activation.UnsupportedDataTypeException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

enum SimpleFSReaderFactory {
    INSTANCE;

    @NotNull
    public SimpleFSReader getReaderForBasePath(@NotNull String path) throws IOException {
        String protocol = PathHelper.getProtocol(path);

        if (protocol == null) {
            return new HDSimpleReader();
        }

        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new MalformedURLException("Cannot build URI");
        }

        if (uri.getScheme().equals("ftp")) {
            try {
                return new FTPSimpleReader(uri);
            } catch (URISyntaxException e) {
                throw new MalformedURLException("Bad URI syntax!");
            }
        }

        throw new IOException(path + "is not supported");
    }

    public boolean isSupportedFileName(@NotNull String fileName) {
        for (SupportedFiles fType : SupportedFiles.values())
            if (fType.isSupportedFileName(fileName))
                return true;

        return false;
    }

    @NotNull
    public SimpleFSReader getReaderForFile(@NotNull Path pathToFile) throws IOException {
        if (!Files.isRegularFile(pathToFile))
            throw new FileNotFoundException("not a file");

        String fileName = pathToFile.getFileName().toString();

        for (SupportedFiles fType : SupportedFiles.values())
            if (fType.isSupportedFileName(fileName))
                return fType.getFileReader(pathToFile);

        throw new UnsupportedDataTypeException(fileName + " is not kinda supported");
    }

    public enum SupportedFiles {
        ZIP {
            @Override
            public boolean isSupportedFileName(@NotNull String fileName) {
                return fileName.matches(".*\\.zip");
            }

            @NotNull
            @Override
            public SimpleFSReader getFileReader(@NotNull Path path) throws IOException {
                return new ZipSimpleReader(path);
            }
        };

        public abstract boolean isSupportedFileName(String fileName);
        public abstract SimpleFSReader getFileReader(Path path) throws IOException;
    }
}
