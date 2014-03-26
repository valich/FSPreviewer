package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;

public class OuterPreviewFrame implements PreviewFrame {
    private final JFrame previewFrame;
    private JComponent previewer;

    public OuterPreviewFrame() {
        previewFrame = new JFrame("preview");
        previewFrame.setLayout(new BorderLayout());
        previewFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        previewFrame.setAlwaysOnTop(true);
        previewFrame.setResizable(true);
        previewFrame.setMinimumSize(new Dimension(200, 200)); // Magic constants, hell yeah
        previewFrame.setMaximumSize(new Dimension(800, 800));
        previewFrame.setUndecorated(true);
    }

    @Override
    public void setPreviewer(@NotNull JComponent previewer) {
        synchronized (this) {
            clearPreviewer();

            this.previewer = previewer;
            for (KeyListener l : previewFrame.getKeyListeners()) {
                previewer.addKeyListener(l);
            }
            previewFrame.getContentPane().add(previewer, BorderLayout.CENTER);
            previewFrame.pack();
            previewFrame.revalidate();
        }
    }

    private void clearPreviewer() {
        if (previewer == null)
            return;

        for (KeyListener l : previewFrame.getKeyListeners()) {
            previewer.removeKeyListener(l);
        }
        previewFrame.getContentPane().remove(previewer);
        previewer = null;
    }

    @Override
    public void show(@Nullable JComponent anchor) {
        if (anchor != null) {
            previewFrame.setLocationRelativeTo(anchor);
        }

        previewFrame.setVisible(true);
    }

    @Override
    public void dispose() {
        clearPreviewer();
        previewFrame.setVisible(false);
    }

    @Override
    public void addKeyListener(@NotNull KeyListener listener) {
        previewFrame.addKeyListener(listener);
    }
}
