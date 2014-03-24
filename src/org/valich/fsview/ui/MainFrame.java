package org.valich.fsview.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by valich on 23.03.14.
 */
public final class MainFrame extends JFrame {
    private JComponent panelHolder;

    private List<FSPanel> panelList = new ArrayList<>();

    public MainFrame() {
        super("fsview");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        panelHolder = new JPanel();
        panelHolder.setLayout(new BorderLayout());

        setJMenuBar(getMenu());

        add(panelHolder, BorderLayout.CENTER);
        add(getLogPanel(), BorderLayout.SOUTH);

        addPanel();
        addPanel();

//        setSize(800, 600);
        pack();
    }


    private JMenuBar getMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu mainMenu = new JMenu("Main");
        mainMenu.add(new JMenuItem("Exit"));
        menuBar.add(mainMenu);

        return menuBar;
    }

    private JComponent getLogPanel() {
        final JTextArea textArea = new JTextArea(5, 80);
        textArea.setEditable(false);
        textArea.setLineWrap(true);

        Logger.getLogger("test").setLevel(Level.FINE);
        Logger.getLogger("test").addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                textArea.append(String.format("[%s] [Thread-%d]: %s.%s -> %s\n", record.getLevel(),
                        record.getThreadID(), record.getSourceClassName(),
                        record.getSourceMethodName(), record.getMessage()));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        return scrollPane;
    }

    private void addPanel() {
        panelList.add(new FSPanel());
        invalidatePanelList();
    }

    private void invalidatePanelList() {
        panelHolder.removeAll();

        JComponent last = panelList.get(0);
        for (int i = 1; i < panelList.size(); ++i) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, last, panelList.get(i));
            splitPane.setResizeWeight(0);
            last = splitPane;
        }
        panelHolder.add(last, BorderLayout.CENTER);
    }

}
