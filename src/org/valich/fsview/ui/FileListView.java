package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.Collection;

/**
 * Created by valich on 25.03.14.
 */
public interface FileListView {

    @Nullable
    FileInfo getSelectedFile();

    @NotNull
    JComponent getContainer();

    void addMouseListener(@NotNull MouseListener listener);

    void addKeyListener(@NotNull KeyListener listener);

    void setDirectoryContents(@NotNull Collection<FileInfo> fileInfos);
}
