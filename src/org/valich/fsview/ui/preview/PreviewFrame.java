package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyListener;

/**
 * Interface describing container for the previewers
 */
public interface PreviewFrame {

    /**
     * Set and place previewer inside of this container
     * @param previewer previewer to be set
     */
    void setPreviewer(@NotNull JComponent previewer);

    /**
     * Show and position container
     * @param anchor component to be positioned relative to, or null if there is not such need
     */
    void show(@Nullable JComponent anchor);

    /**
     * Hide container and remove previewer from it, if any
     */
    void dispose();

    /**
     * Adds key listener to the container
     * @param listener listener to be added
     */
    void addKeyListener(@NotNull KeyListener listener);
}
