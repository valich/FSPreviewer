package org.valich.fsview.ui.preview;

import javax.swing.*;
import java.awt.*;

class LoadingPreviewer extends JComponent {

    public LoadingPreviewer(Dimension previewSize) {
        setLayout(new BorderLayout());

        JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setIndeterminate(true);
        add(progressBar, BorderLayout.CENTER);
    }
}
