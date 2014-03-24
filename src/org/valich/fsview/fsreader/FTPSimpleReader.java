package org.valich.fsview.fsreader;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.valich.fsview.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by valich on 21.03.14.
 */
final class FTPSimpleReader extends AbstractSimpleFSReader {
    private final String baseUri;
    private FTPClient client;

    FTPSimpleReader(URI uri) throws IOException, URISyntaxException {
        super(FileSystems.getFileSystem(new URI("file:///")));

        baseUri = uri.getAuthority();

        client = new FTPClient();
        client.connect(baseUri);
        client.login("anonymous", "12345");
    }

    @Override public synchronized boolean changeDirectory(Path path) {
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

    private Collection<FileInfo> convertFTPList(FTPFile[] ftpFiles) {
        Collection<FileInfo> result = new ArrayList<>();
        for (FTPFile f : ftpFiles) {
            result.add(FileInfo.valueOf(f));
        }
        return result;
    }

    @Override
    public Collection<? extends FileInfo> getDirectoryContents() {
        Collection<FileInfo> result = new ArrayList<>();

        try {
            result.addAll(convertFTPList(client.listDirectories()));
            result.addAll(convertFTPList(client.listFiles()));
        }
        catch (IOException e) {
            e.printStackTrace();
            return result;
        }

        return result;
    }

    @Override
    public FileInfo getFileByPath(Path path) {
        try {
            return FileInfo.valueOf(client.mlistFile(path.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InputStream retrieveFileInputStream(Path path) {
        try {
            return client.retrieveFileStream(path.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
