package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;
import org.valich.fsview.fsreader.FSReader;
import org.valich.fsview.fsreader.IncrementalCompositingFSReader;
import org.valich.fsview.ui.preview.OuterPreviewFrame;
import org.valich.fsview.ui.preview.PreviewComponentFactory;
import org.valich.fsview.ui.preview.PreviewFrame;

import javax.activation.UnsupportedDataTypeException;
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
    private final History<String> browsingHistory;

    private final JButton backButton;
    private final JButton forwardButton;
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
        browsingHistory = new History<>();
        updateCurrentDirFiles();
        previewFrame = new OuterPreviewFrame(PREVIEW_MIN_SIZE, PREVIEW_MAX_SIZE);
        curDirTextField = new JTextField();
        backButton = new JButton();
        forwardButton = new JButton();
        stopChDirButton = new JButton();

        setFileListViewStyle(); // table only
        setUpFileListListeners();
        setUpPreviewFrameListeners();
        fileListView.setDirectoryContents(currentDirFiles);
        browsingHistory.add(fsReader.getWorkingDirectory());

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
        backButton.setText("<");
        backButton.setEnabled(false);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (browsingHistory.getPosition() <= 0)
                    return;
                processChangeDir(browsingHistory.back(), false);
                revalidateHistoryButtons();
            }
        });

        forwardButton.setText(">");
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (browsingHistory.getPosition() + 1 == browsingHistory.getSize())
                    return;
                processChangeDir(browsingHistory.forward(), false);
                revalidateHistoryButtons();
            }
        });

        curDirTextField.setText(fsReader.getWorkingDirectory());
        curDirTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newDir = curDirTextField.getText();
                processChangeDir(newDir, true);
            }
        });

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

        JToolBar toolBar = new JToolBar(SwingConstants.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        toolBar.add(backButton);
        toolBar.add(forwardButton);
        toolBar.add(curDirTextField);
        toolBar.add(stopChDirButton);

        return toolBar;
    }

    private void revalidateHistoryButtons() {
        backButton.setEnabled(browsingHistory.getPosition() > 0);
        forwardButton.setEnabled(browsingHistory.getPosition() + 1 < browsingHistory.getSize());
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
        fileListView.getContainer().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Left-button double-click
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    FileInfo f = fileListView.getSelectedFile();
                    if (f == null)
                        return;
                    processChangeDir(f.getName(), true);
                }
            }
        });

        fileListView.getContainer().getActionMap().put("Enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileInfo f = fileListView.getSelectedFile();
                if (f == null)
                    return;

                processChangeDir(f.getName(), true);
            }
        });
        fileListView.getContainer().getActionMap().put("Preview", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileInfo f = fileListView.getSelectedFile();
                if (f == null)
                    return;

                processPreviewFile(f.getName());
            }
        });

        fileListView.getContainer().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "Preview");
        fileListView.getContainer().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
    }

    private void processChangeDir(@NotNull final String fileName, final boolean writeHistory) {
        Logger.getLogger("test").fine("cd " + fileName);

        final String curDir = fsReader.getWorkingDirectory();
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
                    Throwable cause = e.getCause();
                    if (cause instanceof UnsupportedDataTypeException) {
                        Logger.getLogger("test").fine("Unsupported: " + cause.getMessage());
                    } else {
                        Logger.getLogger("test").warning("Exception: " + cause.getMessage());
                        e.printStackTrace();
                    }
                }
                finally {
                    if (hasChangedDir) {
                        String newDir = fsReader.getWorkingDirectory();
                        curDirTextField.setText(newDir);
                        if (writeHistory) {
                            browsingHistory.add(newDir);
                            revalidateHistoryButtons();
                        }

                        fileListView.setDirectoryContents(currentDirFiles);
                        fileListView.setSelectedFile(findPreviousDir(curDir, newDir));
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

    @NotNull
    private FileInfo findPreviousDir(@NotNull final String prevPath, @NotNull final String curPath) {
        if (!prevPath.startsWith(curPath))
            return FileInfo.UP_DIR;

        String diff = prevPath.substring(curPath.length());
        String clear = diff.replaceAll("[/\\\\]", "");

        for (FileInfo f : currentDirFiles) {
            if (f.getName().equals(clear)) {
                return f;
            }
        }
        return FileInfo.UP_DIR;
    }

}
