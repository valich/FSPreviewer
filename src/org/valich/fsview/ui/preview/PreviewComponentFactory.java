package org.valich.fsview.ui.preview;

import org.jetbrains.annotations.NotNull;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public enum PreviewComponentFactory {
    INSTANCE;

    public JComponent getComponentForFile(@NotNull FileInfo f, @NotNull InputStream is, @NotNull Dimension preferredSize)
                                                                            throws IOException {
        String fileName = f.getName();

        for (SupportedFiles fType : SupportedFiles.values()) {
            if (fType.isSupportedFileName(fileName)) {
                return fType.getPreviewer(fileName, is, preferredSize);
            }
        }

        return new StubFilePreviewer(f, preferredSize);
//        throw new IllegalArgumentException(fileName + " is not supported for preview!");
    }

    public JComponent getComponentForDir(@NotNull FileInfo f, @NotNull Dimension preferredSize) {
        return new StubFilePreviewer(f, preferredSize);
    }

    public JComponent getComponentForFailure(@NotNull Dimension previewSize) {
        return new FailurePreviewer(previewSize);
    }

    public JComponent getComponentForLoading(Dimension previewSize) {
        return new LoadingPreviewer(previewSize);
    }


    public enum SupportedFiles {
        IMAGE {
            private final Pattern PATTERN = Pattern.compile(".*\\.(jpg|jpeg|gif|png|bmp)", Pattern.CASE_INSENSITIVE);

            @Override
            public boolean isSupportedFileName(String fileName) {
                return PATTERN.matcher(fileName).matches();
            }

            @Override
            public JComponent getPreviewer(String fileName, InputStream is, Dimension preferredSize) throws IOException {
                return new SimpleImagePreviewer(is, preferredSize);
            }
        },
        TEXT {
            private final Pattern PATTERN = Pattern.compile(".*\\.(txt|rtf)", Pattern.CASE_INSENSITIVE);

            @Override
            public boolean isSupportedFileName(String fileName) {
                return PATTERN.matcher(fileName).matches();
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
