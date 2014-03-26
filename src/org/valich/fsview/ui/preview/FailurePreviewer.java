package org.valich.fsview.ui.preview;

import javax.swing.*;
import java.awt.*;

class FailurePreviewer extends JPanel {
    public FailurePreviewer(Dimension previewSize) {
        setLayout(new BorderLayout());
        add(new JLabel("FAIL", JLabel.CENTER), BorderLayout.CENTER);
        setAlignmentX((float)0.5);
    }
}
