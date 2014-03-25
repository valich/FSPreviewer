package org.valich.fsview.fsreader;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

final class FTPSimpleReader extends AbstractSimpleFSReader {
    private FTPClient client;

    FTPSimpleReader(@NotNull URI uri) throws IOException, URISyntaxException {
        super(FileSystems.getFileSystem(new URI("file:///")));

        String baseUri = uri.getAuthority();

        client = new FTPClient();
        client.connect(baseUri);
        client.login("anonymous", "12345");
    }

    @Override public synchronized boolean changeDirectory(@NotNull Path path) {
        try {
            if (!client.changeWorkingDirectory(path.toString())) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return super.changeDirectory(path);
    }

    @NotNull
    private Collection<FileInfo> convertFTPList(@NotNull FTPFile[] ftpFiles) {
        Collection<FileInfo> result = new ArrayList<>();
        for (FTPFile f : ftpFiles) {
            result.add(FileInfo.valueOf(f));
        }
        return result;
    }

    @NotNull
    @Override
    public Collection<FileInfo> getDirectoryContents() {
        Collection<FileInfo> result = new ArrayList<>();

        try {
            client.enterLocalPassiveMode();
//            result.addAll(convertFTPList(client.listDirectories()));
            result.addAll(convertFTPList(client.listFiles()));
        }
        catch (IOException e) {
            e.printStackTrace();
            return result;
        }

        return result;
    }

    @Override
    public FileInfo getFileByPath(@NotNull Path path) {
        try {
            client.enterLocalPassiveMode();
            FTPFile[] result = client.listFiles(path.toString());
            if (result.length != 1)
                return null;
            return FileInfo.valueOf(result[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InputStream retrieveFileInputStream(@NotNull Path path) {
        try {
            client.enterLocalPassiveMode();
            return client.retrieveFileStream(path.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
