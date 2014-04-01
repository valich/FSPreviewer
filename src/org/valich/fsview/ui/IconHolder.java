package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum IconHolder {
    INSTANCE;

    private final Map<String, Icon> iconMap = new ConcurrentHashMap<>();

    @Nullable
    private Icon getIcon(@NotNull String path) {
        if (iconMap.containsKey(path)) {
            return iconMap.get(path);
        }
        URL url = getClass().getResource(path);

        Icon result = null;
        if (url != null) {
            result = new ImageIcon(url);
            iconMap.put(path, result);
        }

        return result;
    }

    @Nullable
    public Icon getIcon(@NotNull Icons icon) {
        return getIcon(icon.getPath());
    }

    @SuppressWarnings("unused")
    public static Icon getScaledIcon(@NotNull Icon icon, @NotNull Dimension size) {
        final double scaleX = size.getWidth() / icon.getIconWidth();
        final double scaleY = size.getHeight() / icon.getIconHeight();

        BufferedImage newImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.scale(scaleX, scaleY);
        icon.paintIcon(null, g2d, 0, 0);
        g2d.dispose();

        return new ImageIcon(newImage);
    }

    public enum Icons {
        DIR_ICON {
            @NotNull
            @Override
            String getPath() {
                return "/images/Gnome-Folder-64.png";
            }
        },
        FILE_ICON {
            @NotNull
            @Override
            String getPath() {
                return "/images/Gnome-Text-X-Preview-64.png";
            }
        },
        ERROR_ICON {
            @NotNull
            @Override
            String getPath() {
                return "/images/Gnome-Dialog-Error-64.png";
            }
        };

        @NotNull
        abstract String getPath();
    }
}
