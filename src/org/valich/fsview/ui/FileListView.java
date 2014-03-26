package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.Collection;

/**
 * Interface describing classes that aid in visually showing lists of files
 * (as lists, trees, preview lists, tables, etc)
 */
public interface FileListView {

    /**
     * Returns file or dir, currenly selected in the underlying UI
     * @return {@link org.valich.fsview.FileInfo} containing file's metadata
     * or null if no file is selected
     */
    @Nullable
    FileInfo getSelectedFile();

    /**
     * Returns `root' container with the component
     * @return {@link javax.swing.JComponent} containing FileListView UI
     */
    @NotNull
    JComponent getContainer();

    /**
     * Adds mouse listener for capturing mouse events on the file list
     * @param listener listener to be added
     */
    void addMouseListener(@NotNull MouseListener listener);

    /**
     * Adds key listener for capturing key events on the file list
     * @param listener listener to be added
     */
    void addKeyListener(@NotNull KeyListener listener);

    /**
     * Change the file list
     * @param fileInfos collection of the files to be set and shown
     */
    void setDirectoryContents(@NotNull Collection<FileInfo> fileInfos);
}
