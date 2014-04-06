package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.util.Collection;

/**
 * Interface describing classes that aid in visually showing lists of files
 * (as lists, trees, preview lists, tables, etc)
 */
public interface FileListView {

    /**
     * Returns file or dir, currenly selected in the underlying UI
     *
     * @return {@link org.valich.fsview.FileInfo} containing file's metadata
     * or null if no file is selected
     */
    @Nullable
    FileInfo getSelectedFile();

    boolean setSelectedFile(@NotNull FileInfo file);

    /**
     * Returns `root' container with the component
     *
     * @return {@link javax.swing.JComponent} containing FileListView UI
     */
    @NotNull
    JComponent getContainer();

    /**
     * Change the file list
     *
     * @param fileInfos collection of the files to be set and shown
     */
    void setDirectoryContents(@NotNull Collection<FileInfo> fileInfos);
}
