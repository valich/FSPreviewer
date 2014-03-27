package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class MainFrame extends JFrame {
    // To prevent from being garbage collected
    @NotNull
    @SuppressWarnings("unused")
    private final Logger logger = Logger.getLogger("test");

    @NotNull
    private final JComponent panelHolder;

    @NotNull
    private final List<FSPanel> panelList = new ArrayList<>();

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

        pack();
    }


    @NotNull
    private JMenuBar getMenu() {
        final MainFrame self = this;

        JMenuBar menuBar = new JMenuBar();

        JMenu mainMenu = new JMenu("Main");

        createRadioButtons(mainMenu);

        mainMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                self.dispose();
            }
        });
        mainMenu.add(exit);

        menuBar.add(mainMenu);
        return menuBar;
    }

    private void createRadioButtons(@NotNull JMenu mainMenu) {
        final ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem radioItem;

        ActionListener selectListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selected = e.getActionCommand();
                for (FSPanel panel : panelList) {
//                    panel.setFilesView(selected);
                }
            }
        };

        radioItem = new JRadioButtonMenuItem("List view", true);
        radioItem.setActionCommand("list");
        radioItem.addActionListener(selectListener);
        buttonGroup.add(radioItem);
        mainMenu.add(radioItem);

        radioItem = new JRadioButtonMenuItem("Icon view", false);
        radioItem.setActionCommand("icon");
        radioItem.addActionListener(selectListener);
        radioItem.setEnabled(false);
        buttonGroup.add(radioItem);
        mainMenu.add(radioItem);
    }

    @NotNull
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
        scrollPane.setBorder(BorderFactory.createTitledBorder("Test log"));
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
            splitPane.setResizeWeight(0.5);
            last = splitPane;
        }
        panelHolder.add(last, BorderLayout.CENTER);
    }

}
