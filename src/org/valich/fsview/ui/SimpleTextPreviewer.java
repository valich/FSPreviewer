package org.valich.fsview.ui;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by valich on 25.03.14.
 */
public final class SimpleTextPreviewer extends JPanel {
    public SimpleTextPreviewer(String fileName, InputStream is, Dimension preferredSize) throws IOException {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        StyledDocument doc;

        switch (extension) {
            case "rtf":
                doc = createRTFDoc(is);
                break;
            case "txt":
                doc = createTXTDoc(is);
                break;
            default:
                throw new AssertionError("Passed pattern but not exact check");
        }

        JTextPane pane = new JTextPane(doc);
        pane.setPreferredSize(preferredSize);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        pane.setFocusable(false);

        pane.setEditable(false);

        JScrollPane scrPane = new JScrollPane(pane);
//        scrPane.setPreferredSize(preferredSize);

        setLayout(new BorderLayout());
        add(scrPane, BorderLayout.CENTER);

        setMaximumSize(preferredSize);
    }

    private StyledDocument createRTFDoc(InputStream is) throws IOException {
        RTFEditorKit rtfKit = new RTFEditorKit();
        StyledDocument doc = (StyledDocument) rtfKit.createDefaultDocument();
        try {
            rtfKit.read(is, doc, 0);
        } catch (BadLocationException e) {
            throw new IOException("Bad location", e.getCause());
        }

        return doc;
    }

    private StyledDocument createTXTDoc(InputStream is) throws IOException {
        DefaultEditorKit txtKit = new DefaultEditorKit();
        StyledDocument doc = new DefaultStyledDocument();
        try {
            txtKit.read(is, doc, 0);
        } catch (BadLocationException e) {
            throw new IOException("Bad location", e.getCause());
        }

        return doc;
    }
}
