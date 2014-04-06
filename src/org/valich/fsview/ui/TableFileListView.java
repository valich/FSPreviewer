package org.valich.fsview.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lists files in a table, containing their name, extension and size
 */
public class TableFileListView implements FileListView {
    private final FSTableModel tableModel;
    private final JTable table;

    TableFileListView() {
        tableModel = new FSTableModel(new ArrayList<FileInfo>());
        table = new JTable(tableModel);

        setUpTable();
    }

    private void setUpTable() {
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setShowVerticalLines(false);

        for (int i = 0; i < 3; ++i) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(i == 0 ? 200 : 50);

            if (i == 2) {
                col.setCellRenderer(new SizeRenderer());
            } else {
                col.setCellRenderer(new RowRenderer());
            }
        }
    }

    @Nullable
    @Override
    public FileInfo getSelectedFile() {
        try {
            int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
            return tableModel.getTableDataElement(selectedRow);
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        return null;
    }

    @Override
    public boolean setSelectedFile(@NotNull FileInfo file) {
        int n = tableModel.getRowCount();
        for (int i = 0; i < n; ++i) {
            if (tableModel.getTableDataElement(i) == file) {
                table.changeSelection(table.convertRowIndexToView(i), 0, false, false);
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public JComponent getContainer() {
        return table;
    }

    @Override
    public void setDirectoryContents(@NotNull Collection<FileInfo> fileInfos) {
        tableModel.setTableData(fileInfos);
    }

    private final static class FSTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Name", "Ext", "Size"};
        @NotNull
        private List<FileInfo> tableData;


        public FSTableModel(@NotNull List<FileInfo> tableData) {
            this.tableData = tableData;
        }

        @NotNull
        FileInfo getTableDataElement(int i) throws ArrayIndexOutOfBoundsException {
            if (i < 0 || i >= getRowCount())
                throw new ArrayIndexOutOfBoundsException(i);

            return tableData.get(i);
        }

        public synchronized void setTableData(@NotNull Collection<FileInfo> tableData) {
            this.tableData = new ArrayList<>(tableData);
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

        @NotNull
        private static String getName(@NotNull FileInfo f) {
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

        @NotNull
        private static String getExtension(@NotNull FileInfo f) {
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

        @NotNull
        private static Long getSize(@NotNull FileInfo f) {
            return f.getSize();
        }
    }

    private static class RowRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);


            if (!isSelected) {
                cellComponent.setBackground(Color.WHITE);
            } else {
                if (table.isFocusOwner()) {
                    cellComponent.setBackground(Color.BLUE);
                } else {
                    cellComponent.setBackground(Color.GRAY);
                }
            }

            return cellComponent;
        }
    }

    private final static class SizeRenderer extends RowRenderer {
        NumberFormat formatter;

        public SizeRenderer() {
            super();

            setHorizontalAlignment(JLabel.RIGHT);

            formatter = NumberFormat.getNumberInstance();
            formatter.setGroupingUsed(true);
        }

        @Override
        public void setValue(Object o) {
            if (o == null) {
                setText("");
            } else {
                long l = ((Long) o);
                if (l == -1) {
                    setText("");
                } else {
                    setText(formatter.format(l));
                }
            }
        }
    }
}
