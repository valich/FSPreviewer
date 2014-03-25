package org.valich.fsview.ui;

import org.valich.fsview.FileInfo;
import org.valich.fsview.fsreader.FSReader;
import org.valich.fsview.fsreader.IncrementalCompositingFSReader;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Created by valich on 23.03.14.
 */
final class FSPanel extends JPanel {
    private final FSReader<String> fsReader;
    private final JTextField textField;
    private final JFrame previewFrame;
    private List<FileInfo> currentDirFiles;
    private FileListView fileListView;

    private SwingWorker<Collection<FileInfo>, Void> changeDirWorker;
    private SwingWorker<JComponent, Void> previewFileWorker;



    public enum ViewStyle {
        TABLE {
            @Override
            public String getStyleName() {
                return "Table";
            }
        },
        PREVIEW {
            @Override
            public String getStyleName() {
                return "Preview";
            }
        };

        public abstract String getStyleName();
    }

    public FSPanel() {
        super();

        fsReader = new IncrementalCompositingFSReader();
        setCurrentDirFilesWithUp(fsReader.getDirectoryContents());

        textField = new JTextField();
        previewFrame = new JFrame("preview");
        fileListView = null;

        setFileListViewStyle(ViewStyle.TABLE);
        setUpPreviewFrame();
        setUpFileListListeners();


        setLayout(new BorderLayout());
        add(getAddressBar(), BorderLayout.NORTH);
        add(new JScrollPane(fileListView.getContainer()), BorderLayout.CENTER);

        this.setVisible(true);
    }

    private void setFileListViewStyle(ViewStyle style) {
        if (style == ViewStyle.TABLE) {
            fileListView = new TableFileListView();
        } else {
            //
        }
    }

    private void setUpPreviewFrame() {
        previewFrame.setLayout(new BorderLayout());
        previewFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        previewFrame.setAlwaysOnTop(true);
        previewFrame.setResizable(true);
        previewFrame.setMinimumSize(new Dimension(200, 200));
        previewFrame.setMaximumSize(new Dimension(800, 800));
        previewFrame.setUndecorated(true);

        previewFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ESCAPE:
                        if (previewFileWorker != null)
                            previewFileWorker.cancel(true);
                        if (previewFrame.isVisible()) {
                            previewFrame.setVisible(false);
                            previewFrame.getContentPane().removeAll();
                        }
                        break;
                }
            }
        });
    }

    private JComponent getAddressBar() {
        textField.setText(fsReader.getWorkingDirectory());
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newDir = textField.getText();
                processChangeDir(newDir);
            }
        });

//        JLayeredPane pane = new JLayeredPane();
//        pane.setLayout(new GridLayout(2, 2));

        JToolBar toolBar = new JToolBar(SwingConstants.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        JButton stopButton = new JButton("X");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (changeDirWorker != null) {
                    Logger.getLogger("test").info("stopping!");
                    changeDirWorker.cancel(true);
                }
            }
        });

        toolBar.add(textField);
        toolBar.add(stopButton);

        return toolBar;
    }

    private synchronized void setCurrentDirFilesWithUp(Collection<? extends FileInfo> files) {
        currentDirFiles = new ArrayList<>();
        currentDirFiles.add(FileInfo.UP_DIR);
        currentDirFiles.addAll(files);
    }

    private void setUpFileListListeners() {
        fileListView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Left-button double-click
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    FileInfo f = fileListView.getSelectedFile();
                    if (f == null)
                        return;
                    processChangeDir(f.getName());
                }
            }
        });
        fileListView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                FileInfo f = fileListView.getSelectedFile();
                if (f == null)
                    return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        processPreviewFile(f.getName());
                        break;
                    case KeyEvent.VK_ENTER:
                        processChangeDir(f.getName());
                        break;
                }
            }
        });
    }

    private void processChangeDir(final String fileName) {
        Logger.getLogger("test").fine("cd " + fileName);

        if (changeDirWorker != null)
            changeDirWorker.cancel(true);

        changeDirWorker = new SwingWorker<Collection<FileInfo>, Void>() {
            @Override
            protected Collection<FileInfo> doInBackground() throws Exception {
                if (fsReader.changeDirectory(fileName)) {
                    return fsReader.getDirectoryContents();
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    Collection<FileInfo> result = get();
                    if (result == null) {
                        Logger.getLogger("test").warning("cd failed");
                        return; // do nothing
                    }

                    setCurrentDirFilesWithUp(fsReader.getDirectoryContents());
                    fileListView.setDirectoryContents(currentDirFiles);

                    String newDir = fsReader.getWorkingDirectory();
                    textField.setText(newDir);
                    Logger.getLogger("test").fine("went to: " + newDir);
                } catch (InterruptedException | CancellationException e) {
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }
        };
        changeDirWorker.execute();

//        if (fsReader.changeDirectory(fileName)) {
//            setCurrentDirFilesWithUp(fsReader.getDirectoryContents());
//            tableModel.setTableData(currentDirFiles);
//
//            String newDir = fsReader.getWorkingDirectory();
//            textField.setText(newDir);
//            Logger.getLogger("test").fine("went to: " + newDir);
//        }
    }

    private void processPreviewFile(final String fileName) {
        Logger.getLogger("test").fine("preview " + fileName);

        if (previewFileWorker != null)
            previewFileWorker.cancel(true);
        final JLabel fuckingLabel = new JLabel("Loading");

        previewFileWorker = new SwingWorker<JComponent, Void>() {
            @Override
            protected JComponent doInBackground() throws Exception {
                FileInfo f = fsReader.getFileByPath(fileName);
                InputStream is = fsReader.retrieveFileInputStream(fileName);
                if (f == null || is == null)
                    return null;

                JComponent result;
                try {
                    result = PreviewComponentFactory.INSTANCE.getComponentForFile(f, is, previewFrame.getMaximumSize());
                }
                catch (IllegalArgumentException e) {
                    Logger.getLogger("test").warning("not supported");
                    return null;
                }
                return result;
            }

            @Override
            public void done() {
                try {
                    JComponent result = get();

                    synchronized (previewFrame) {
                        JComponent newComp;
                        if (result == null) {
                            newComp = new JLabel("FAILED");
                        } else {
                            newComp = result;
                        }

                        updateContents(previewFrame, newComp);
                        previewFrame.setLocationRelativeTo(fileListView.getContainer());
                    }
                } catch (InterruptedException | CancellationException e) {
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };

        synchronized (previewFrame) {
            updateContents(previewFrame, fuckingLabel);
            previewFrame.setVisible(true);
        }
        previewFileWorker.execute();
    }

    private void updateContents(JFrame frame, JComponent comp) {
        frame.getContentPane().removeAll();
        frame.getContentPane().add(comp, BorderLayout.CENTER);
        frame.pack();
        frame.revalidate();
//        frame.update(frame.getGraphics());
    }


}
