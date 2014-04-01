package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;
import org.valich.fsview.fsreader.FSReader;
import org.valich.fsview.fsreader.IncrementalCompositingFSReader;
import org.valich.fsview.ui.preview.OuterPreviewFrame;
import org.valich.fsview.ui.preview.PreviewComponentFactory;
import org.valich.fsview.ui.preview.PreviewFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Panel purposed for traversing file systems and previewing files and dirs inside
 */
final class FSPanel extends JPanel {
    private final Dimension PREVIEW_MAX_SIZE = new Dimension(600, 600);
    private final Dimension PREVIEW_MIN_SIZE = new Dimension(200, 200);

    private final FSPanel self = this;
    private final FSReader<String> fsReader;
    private final JTextField curDirTextField;
    private final JButton stopChDirButton;
    private final PreviewFrame previewFrame;

    @NotNull
    private FileListView fileListView;

    @NotNull
    private List<FileInfo> currentDirFiles;

    @Nullable
    private SwingWorker<Collection<FileInfo>, Void> changeDirWorker;
    @Nullable
    private SwingWorker<JComponent, Void> previewFileWorker;

    public FSPanel() {
        super();

        fsReader = new IncrementalCompositingFSReader();
        updateCurrentDirFiles();
        previewFrame = new OuterPreviewFrame(PREVIEW_MIN_SIZE, PREVIEW_MAX_SIZE);
        curDirTextField = new JTextField();
        stopChDirButton = new JButton();

        setFileListViewStyle(); // table only
        setUpFileListListeners();
        setUpPreviewFrameListeners();
        fileListView.setDirectoryContents(currentDirFiles);

        setLayout(new BorderLayout());
        add(getAddressBar(), BorderLayout.NORTH);
        add(new JScrollPane(fileListView.getContainer()), BorderLayout.CENTER);

        this.setVisible(true);
    }

    private void setFileListViewStyle() {
        fileListView = new TableFileListView();
    }

    private void setUpPreviewFrameListeners() {
        previewFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ESCAPE:
                        if (previewFileWorker != null)
                            previewFileWorker.cancel(true);

                        previewFrame.dispose();
                        break;
                }
            }
        });
    }

    @NotNull
    private JComponent getAddressBar() {
        curDirTextField.setText(fsReader.getWorkingDirectory());
        curDirTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newDir = curDirTextField.getText();
                processChangeDir(newDir);
            }
        });

//        JLayeredPane pane = new JLayeredPane();
//        pane.setLayout(new GridLayout(2, 2));

        JToolBar toolBar = new JToolBar(SwingConstants.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        stopChDirButton.setText("X");
        stopChDirButton.setEnabled(false);
        stopChDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (changeDirWorker != null) {
                    Logger.getLogger("test").info("stopping!");
                    changeDirWorker.cancel(true);
                }
            }
        });

        toolBar.add(curDirTextField);
        toolBar.add(stopChDirButton);

        return toolBar;
    }

    private synchronized void updateCurrentDirFiles() {
        clearCurrentDirFiles();

        try {
            currentDirFiles.addAll(fsReader.getDirectoryContents());
        } catch (IOException e) {
            Logger.getLogger("test").warning("Exception while getting dir contents: " + e.getMessage());
//            Logger.getLogger("test").warning(Arrays.toString(e.getStackTrace()));
        }
    }

    private synchronized void updateCurrentDirFiles(Collection<FileInfo> dirContents) {
        clearCurrentDirFiles();

        currentDirFiles.addAll(dirContents);
    }

    private synchronized void clearCurrentDirFiles() {
        currentDirFiles = new ArrayList<>();
        currentDirFiles.add(FileInfo.UP_DIR);
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

    private void processChangeDir(@NotNull final String fileName) {
        Logger.getLogger("test").fine("cd " + fileName);

        if (changeDirWorker != null)
            changeDirWorker.cancel(true);

        changeDirWorker = new SwingWorker<Collection<FileInfo>, Void>() {
            private volatile boolean hasChangedDir = false;

            @Override
            protected Collection<FileInfo> doInBackground() throws Exception {
                if (fsReader.changeDirectory(fileName)) {
                    hasChangedDir = true;
                    return fsReader.getDirectoryContents();
                }
                return null;
            }

            @Override
            public void done() {
                if (hasChangedDir)
                    clearCurrentDirFiles();

                try {
                    Collection<FileInfo> result = get();
                    if (result == null) {
                        Logger.getLogger("test").warning("cd failed (false)");
                        return; // do nothing
                    }

                    updateCurrentDirFiles(result);
                } catch (InterruptedException | CancellationException ignored) {
                } catch (ExecutionException e) {
                    Logger.getLogger("test").warning("Exception: " + e.getCause().getMessage());
                    e.printStackTrace();
                }
                finally {
                    if (hasChangedDir) {
                        fileListView.setDirectoryContents(currentDirFiles);

                        String newDir = fsReader.getWorkingDirectory();
                        curDirTextField.setText(newDir);
                        Logger.getLogger("test").fine("went to: " + newDir);
                    }

                    stopChDirButton.setEnabled(false);
                    stopChDirButton.setBackground(Color.WHITE);
                }
            }
        };

        stopChDirButton.setEnabled(true);
        stopChDirButton.setBackground(Color.RED);
        changeDirWorker.execute();
    }

    private void processPreviewFile(@NotNull final String fileName) {
        Logger.getLogger("test").fine("preview " + fileName);

        if (previewFileWorker != null)
            previewFileWorker.cancel(true);

        previewFileWorker = new SwingWorker<JComponent, Void>() {
            @Override
            protected JComponent doInBackground() throws Exception {
                FileInfo f = fsReader.getFileByPath(fileName);

                if (!f.getAttributes().contains(FileInfo.FileAttribute.IS_REGULAR_FILE)) {
                    return PreviewComponentFactory.INSTANCE.getComponentForDir(f);
                }

                InputStream is = fsReader.retrieveFileInputStream(fileName);

                return PreviewComponentFactory.INSTANCE.getComponentForFile(f, is, PREVIEW_MAX_SIZE);
            }

            @Override
            public void done() {
                boolean cancelled = false;

                JComponent internal = null;
                try {
                    JComponent result = get();
                    if (result == null) {
                        internal = PreviewComponentFactory.INSTANCE.getComponentForFailure();
                    } else {
                        internal = result;
                    }


                } catch (InterruptedException | CancellationException e) {
                    cancelled = true;
                } catch (ExecutionException e) {
                    Logger.getLogger("test").warning("Exception: " + e.getCause().getMessage());
                    internal = PreviewComponentFactory.INSTANCE.getComponentForFailure();
                } finally {
                    if (!cancelled && internal != null) {
                        previewFrame.setPreviewer(internal);
                        previewFrame.show(self);
                    }
                }
            }
        };

        previewFrame.setPreviewer(PreviewComponentFactory.INSTANCE.getComponentForLoading());
        previewFrame.show(self);

        previewFileWorker.execute();
    }

}
