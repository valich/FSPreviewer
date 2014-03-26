package org.valich.fsview.ui.preview;

import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.awt.*;

class StubFilePreviewer extends JPanel {
    public StubFilePreviewer(FileInfo f, Dimension preferredSize) {
        setLayout(new BorderLayout());

        String northStr = "";
        if (f.getAttributes().contains(FileInfo.FileAttribute.IS_SYMLINK)) {
            northStr = "Symlink to ";
        }

        if (f.getAttributes().contains(FileInfo.FileAttribute.IS_DIRECTORY)) {
            northStr = northStr + "Directory";
        } else {
            northStr = northStr + "File";
        }

        add(new JLabel(northStr, JLabel.CENTER), BorderLayout.NORTH);
        add(new JLabel(f.getName(), JLabel.CENTER), BorderLayout.CENTER);

        if (f.getSize() != -1)
            add(new JLabel("" + f.getSize() + " bytes", JLabel.CENTER), BorderLayout.SOUTH);
    }
}
