package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by valich on 25.03.14.
 */
public enum PreviewComponentFactory {
    INSTANCE;

    public JComponent getComponentForFile(@NotNull FileInfo f, @NotNull InputStream is, @NotNull Dimension preferredSize)
                                                                            throws IllegalArgumentException, IOException {
        String fileName = f.getName();

        for (SupportedFiles fType : SupportedFiles.values()) {
            if (fType.isSupportedFileName(fileName)) {
                return fType.getPreviewer(fileName, is, preferredSize);
            }
        }

        throw new IllegalArgumentException(fileName + " is not supported for preview!");
    }

    public enum SupportedFiles {
        IMAGE {
            private Pattern pattern = Pattern.compile(".*\\.(jpg|jpeg|gif|png|bmp)", Pattern.CASE_INSENSITIVE);

            @Override
            public boolean isSupportedFileName(String fileName) {
                return pattern.matcher(fileName).matches();
            }

            @Override
            public JComponent getPreviewer(String fileName, InputStream is, Dimension preferredSize) throws IOException {
                return new SimpleImagePreviewer(is, preferredSize);
            }
        },
        TEXT {
            private Pattern pattern = Pattern.compile(".*\\.(txt|rtf)", Pattern.CASE_INSENSITIVE);

            @Override
            public boolean isSupportedFileName(String fileName) {
                return pattern.matcher(fileName).matches();
            }

            @Override
            public JComponent getPreviewer(String fileName, InputStream is, Dimension preferredSize) throws IOException {
                return new SimpleTextPreviewer(fileName, is, preferredSize);
            }
        };

        public abstract boolean isSupportedFileName(String fileName);

        public abstract JComponent getPreviewer(String fileName, InputStream is, Dimension preferredSize) throws IOException;
    }
}
