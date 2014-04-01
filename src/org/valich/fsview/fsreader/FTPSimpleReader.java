package org.valich.fsview.fsreader;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

final class FTPSimpleReader extends AbstractSimpleFSReader {
    private final FTPClient client;

    FTPSimpleReader(@NotNull URI uri) throws IOException {
        super(new UnixPseudoPath().getFileSystem());

        String baseUri = uri.getAuthority();

        client = new FTPClient();
        client.connect(baseUri);
        client.login("anonymous", "12345");
    }

    @Override public synchronized boolean changeDirectory(@NotNull Path path) throws IOException {
        if (!client.changeWorkingDirectory(path.toString())) {
            return false;
        }

        return super.changeDirectory(path);
    }

    @NotNull
    @Override
    public Collection<FileInfo> getDirectoryContents() throws IOException {
        Collection<FileInfo> result = new ArrayList<>();

        client.enterLocalPassiveMode();
        result.addAll(convertFTPList(client.listFiles()));

        return result;
    }

    @NotNull
    @Override
    public FileInfo getFileByPath(@NotNull Path path) throws IOException {
        client.enterLocalPassiveMode();
        FTPFile[] result = client.listFiles(path.toString());
        if (result.length == 0)
            throw new IOException("There is no file linked to that path " + path);
        return FileInfo.valueOf(result[0]);
    }

    @NotNull
    @Override
    public InputStream retrieveFileInputStream(@NotNull Path path) throws IOException {
        client.enterLocalPassiveMode();
        return client.retrieveFileStream(path.toString());
    }

    @NotNull
    private Collection<FileInfo> convertFTPList(@NotNull FTPFile[] ftpFiles) {
        Collection<FileInfo> result = new ArrayList<>();
        for (FTPFile f : ftpFiles) {
            result.add(FileInfo.valueOf(f));
        }
        return result;
    }

}
