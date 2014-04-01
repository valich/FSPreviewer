package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

final class SimpleImagePreviewer extends JPanel {

    public SimpleImagePreviewer(@NotNull InputStream is, @NotNull Dimension preferredSize) throws IOException {
        MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(is);
        BufferedImage img = ImageIO.read(imageInputStream);

        int w = img.getWidth();
        int h = img.getHeight();

        Image result;
        if (w > preferredSize.getWidth() || h > preferredSize.getHeight()) {
            double maxw = preferredSize.getWidth();
            double maxh = preferredSize.getHeight();
            double ratio = Math.max(h / maxh, w / maxw);
            int neww = (int) (w / ratio);
            int newh = (int) (h / ratio);

            result = img.getScaledInstance(neww, newh, Image.SCALE_SMOOTH);
        } else {
            result = img;
        }

        setLayout(new BorderLayout());
        add(new JLabel(new ImageIcon(result)), BorderLayout.CENTER);
    }
}
