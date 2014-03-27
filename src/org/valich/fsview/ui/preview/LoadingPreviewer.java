package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class LoadingPreviewer extends JComponent {

    public LoadingPreviewer(@NotNull Dimension previewSize) {
        setLayout(new BorderLayout());

        JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setIndeterminate(true);
        add(progressBar, BorderLayout.CENTER);
    }
}
