package org.valich.fsview.ui;

import org.valich.fsview.FileInfo;
import org.valich.fsview.fsreader.FSReader;
import org.valich.fsview.fsreader.IncrementalCompositingFSReader;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Created by valich on 23.03.14.
 */
final class FSPanel extends JPanel {
    private final JTable table;
    private final FSTableModel tableModel;
    private final FSReader<String> fsReader;
    private final JTextField textField;
    private List<FileInfo> currentDirFiles;

    private SwingWorker<Collection<? extends FileInfo>, Void> changeDirWorker;

    public FSPanel() {
        super();

        fsReader = new IncrementalCompositingFSReader();
        setCurrentDirFilesWithUp(fsReader.getDirectoryContents());

        tableModel = new FSTableModel(currentDirFiles);
        table = new JTable(tableModel);
        setUpTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        textField = getAddressBar();

        setLayout(new BorderLayout());
        add(textField, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        this.setVisible(true);
    }

    private JTextField getAddressBar() {
        final JTextField result = new JTextField(fsReader.getWorkingDirectory());
        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newDir = result.getText();
                processChangeDir(newDir);
            }
        });
        return result;
    }

    private synchronized void setCurrentDirFilesWithUp(Collection<? extends FileInfo> files) {
        currentDirFiles = new ArrayList<>();
        currentDirFiles.add(FileInfo.UP_DIR);
        currentDirFiles.addAll(files);
    }

    private void setUpTable(final JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < 3; ++i) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(i == 0 ? 200 : 50);
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Left-button double-click
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    processChangeDir(getSelectedFile().getName());
                }
            }
        });
    }

    private synchronized FileInfo getSelectedFile() {
        int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
        return currentDirFiles.get(selectedRow);
    }

    private void processChangeDir(final String fileName) {
        Logger.getLogger("test").fine("cd " + fileName);

        if (changeDirWorker != null) {
            changeDirWorker.cancel(true);
        }
        changeDirWorker = new SwingWorker<Collection<? extends FileInfo>, Void>() {
            @Override
            protected Collection<? extends FileInfo> doInBackground() throws Exception {
                if (fsReader.changeDirectory(fileName)) {
                    return fsReader.getDirectoryContents();
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    Collection<? extends FileInfo> result = get();
                    if (result == null)
                        return; // do nothing

                    setCurrentDirFilesWithUp(fsReader.getDirectoryContents());
                    tableModel.setTableData(currentDirFiles);

                    String newDir = fsReader.getWorkingDirectory();
                    textField.setText(newDir);
                    Logger.getLogger("test").fine("went to: " + newDir);
                } catch (InterruptedException e) {
                } catch (CancellationException e) {
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

    final static class FSTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Name", "Ext", "Size"};
        private List<FileInfo> tableData;


        public FSTableModel(List<FileInfo> tableData) {
            this.tableData = tableData;
        }

        public synchronized void setTableData(List<FileInfo> tableData) {
            this.tableData = tableData;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return tableData.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= tableData.size())
                return null;

            FileInfo fileInfo = tableData.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return getName(fileInfo);
                case 1:
                    return getExtension(fileInfo);
                case 2:
                    return getSize(fileInfo);
            }

            return null;
        }

        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public String getColumnName(int coln) {
            return COLUMN_NAMES[coln];
        }

        private static String getName(FileInfo f) {
            String name = f.getName();
            if (name.charAt(0) == '.')
                return name;

            int point = name.lastIndexOf(".");
            if (point == -1)
                point = name.length();

            if (f.getAttributes().contains(FileInfo.FileAttribute.IS_REGULAR_FILE))
                return name.substring(0, point);
            else
                return name;
        }

        private static String getExtension(FileInfo f) {
            if (f.getAttributes().contains(FileInfo.FileAttribute.IS_REGULAR_FILE)) {
                String name = f.getName();
                if (name.charAt(0) == '.')
                    return "";

                int point = name.lastIndexOf(".");
                if (point == -1)
                    return "";
                else
                    return name.substring(point + 1);
            }
            else if (f.getAttributes().contains(FileInfo.FileAttribute.IS_DIRECTORY))
                return "<DIR>";
            else
                return "";
        }

        private static Long getSize(FileInfo f) {
            return f.getSize();
        }
    }
}
