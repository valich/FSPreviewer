package org.valich.fsview.ui.preview;

import org.valich.fsview.ui.IconHolder;

import javax.swing.*;
import java.awt.*;

class FailurePreviewer extends JPanel {
    public FailurePreviewer() {
        setLayout(new BorderLayout());

        Icon icon = IconHolder.INSTANCE.getIcon(IconHolder.Icons.ERROR_ICON);

        add(new JLabel(icon, JLabel.CENTER), BorderLayout.CENTER);
        setAlignmentX((float) 0.5);
    }
}
