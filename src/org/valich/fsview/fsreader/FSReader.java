package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author valich
 *
 * General interface describing classes that are able to traverse some filesystems
 * (hard disk, network, archive, etc) and retrieve files (data) from there
 */
public interface FSReader<T> {
    /**
     * lists files and directories contained in the current directory
     * @return collection of the files and dirs contained
     * @throws java.io.IOException if some problem occurred while working with FS
     */
    @NotNull
    Collection<FileInfo> getDirectoryContents() throws IOException;

    /**
     * returns metadata for specified file
     * @param pathName path to the file
     * @return {@link org.valich.fsview.FileInfo} containing metadata
     * @throws java.io.IOException if file could not be opened
     */
    @NotNull
    FileInfo getFileByPath(@NotNull T pathName) throws IOException;

    /**
     * changes current directory to the specified by path
     * @param pathName path to the directory
     * @return true if operation succeeded and false otherwise
     */
    boolean changeDirectory(@NotNull T pathName) throws IOException;

    /**
     * pwd
     * @return path to the current directory
     */
    @NotNull
    T getWorkingDirectory();

    /**
     * Retrieves a stream with which one can read file contents
     * @param pathName path to the file which is to be read
     * @return readable stream with file contents
     * @throws java.io.IOException if file could not be opened for reading
     */
    @NotNull
    InputStream retrieveFileInputStream(@NotNull T pathName) throws IOException;
}
