package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;
import org.valich.fsview.ui.IconHolder;

import javax.swing.*;
import java.awt.*;

class StubFilePreviewer extends JPanel {
    public StubFilePreviewer(@NotNull FileInfo f) {
        setLayout(new BorderLayout());

        String northStr = "";
        if (f.getAttributes().contains(FileInfo.FileAttribute.IS_SYMLINK)) {
            northStr = "Symlink to ";
        }

        Icon icon;
        if (f.getAttributes().contains(FileInfo.FileAttribute.IS_DIRECTORY)) {
            northStr = northStr + "Directory";
            icon = IconHolder.INSTANCE.getIcon(IconHolder.Icons.DIR_ICON);
        } else {
            northStr = northStr + "File";
            icon = IconHolder.INSTANCE.getIcon(IconHolder.Icons.FILE_ICON);
        }

        add(new JLabel(northStr, JLabel.CENTER), BorderLayout.NORTH);
        add(new JLabel(f.getName(), icon, JLabel.CENTER), BorderLayout.CENTER);

        if (f.getSize() != -1)
            add(new JLabel("" + f.getSize() + " bytes", JLabel.CENTER), BorderLayout.SOUTH);
    }
}
