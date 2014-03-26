package org.valich.fsview.ui.preview;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

final class SimpleImagePreviewer extends JPanel {

    public SimpleImagePreviewer(InputStream is, Dimension preferredSize) throws IOException {
        MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(is);
        BufferedImage img = ImageIO.read(imageInputStream);

        int w = img.getWidth();
        int h = img.getHeight();
        double maxw = preferredSize.getWidth();
        double maxh = preferredSize.getHeight();
        double ratio = Math.max(h / maxh, w / maxw);
        int neww = (int)(w / ratio);
        int newh = (int)(h / ratio);

        Image result = img.getScaledInstance(neww, newh, Image.SCALE_SMOOTH);

        setLayout(new BorderLayout());
        add(new JLabel(new ImageIcon(result)), BorderLayout.CENTER);
    }
}