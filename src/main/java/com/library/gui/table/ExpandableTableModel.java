package com.library.gui.table;

import javax.swing.table.AbstractTableModel;
import javax.swing.ImageIcon;
import java.util.*;

public class ExpandableTableModel extends AbstractTableModel {
    private String[] columnNames;
    private List<Object[]> data;
    private Set<Integer> expandedRows;
    private Map<Integer, Object[]> detailData;

    public ExpandableTableModel(String[] columnNames) {
        this.columnNames = columnNames;
        this.data = new ArrayList<>();
        this.expandedRows = new HashSet<>();
        this.detailData = new HashMap<>();
    }

    public void addRow(Object[] rowData) {
        data.add(rowData);
        fireTableDataChanged();
    }

    public void removeRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < data.size()) {
            data.remove(rowIndex);
            // Remove any detail data for this row
            detailData.remove(rowIndex);
            // Adjust expanded rows
            Set<Integer> newExpandedRows = new HashSet<>();
            for (int row : expandedRows) {
                if (row < rowIndex) {
                    newExpandedRows.add(row);
                } else if (row > rowIndex) {
                    newExpandedRows.add(row - 1);
                }
            }
            expandedRows = newExpandedRows;
            fireTableDataChanged();
        }
    }

    public void clear() {
        data.clear();
        expandedRows.clear();
        detailData.clear();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        int totalRows = data.size();
        for (int i = 0; i < data.size(); i++) {
            if (expandedRows.contains(i)) {
                totalRows++; 
            }
        }
        return totalRows;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int actualRow = getActualRow(rowIndex);

        if (actualRow == -1) {
            // This is a detail row
            int parentRow = getParentRow(rowIndex);
            if (detailData.containsKey(parentRow) && columnIndex < detailData.get(parentRow).length) {
                return detailData.get(parentRow)[columnIndex];
            }
            return "";
        }

        // This is a regular row
        if (actualRow < data.size() && columnIndex < data.get(actualRow).length) {
            return data.get(actualRow)[columnIndex];
        }

        return null;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 1) { 
            return ImageIcon.class;
        }
        return Object.class;
    }

    public int getActualRow(int viewRow) {
        int currentRow = 0;
        for (int i = 0; i < data.size(); i++) {
            if (viewRow == currentRow) {
                return i;
            }
            currentRow++;

            if (expandedRows.contains(i)) {
                if (viewRow == currentRow) {
                    return -1; 
                }
                currentRow++;
            }
        }
        return -1;
    }

    public int getParentRow(int detailRow) {
        int currentRow = 0;
        for (int i = 0; i < data.size(); i++) {
            if (detailRow == currentRow + 1 && expandedRows.contains(i)) {
                return i;
            }
            currentRow++;
            if (expandedRows.contains(i)) {
                currentRow++;
            }
        }
        return -1;
    }

    public void toggleRowExpansion(int actualRow) {
        if (expandedRows.contains(actualRow)) {
            expandedRows.remove(actualRow);
            detailData.remove(actualRow);
        } else {
            expandedRows.add(actualRow);
        }
        fireTableDataChanged();
    }

    public boolean isRowExpanded(int actualRow) {
        return expandedRows.contains(actualRow);
    }

    public void setDetailData(int actualRow, Object[] detailRow) {
        detailData.put(actualRow, detailRow);
        fireTableDataChanged();
    }

    public void collapseAll() {
        expandedRows.clear();
        detailData.clear();
        fireTableDataChanged();
    }

    public void expandAll() {
        for (int i = 0; i < data.size(); i++) {
            expandedRows.add(i);
        }
        fireTableDataChanged();
    }

    public int getViewRowForActualRow(int actualRow) {
        int viewRow = actualRow;
        for (int i = 0; i < actualRow; i++) {
            if (expandedRows.contains(i)) {
                viewRow++;
            }
        }
        return viewRow;
    }
}