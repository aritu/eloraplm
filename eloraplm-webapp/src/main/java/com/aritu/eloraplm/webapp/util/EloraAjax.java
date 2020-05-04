/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.webapp.util;

import static java.lang.String.format;
import java.util.Collection;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;

public final class EloraAjax {

    private EloraAjax() {
    }

    public static PartialViewContext getContext() {
        return FacesContext.getCurrentInstance().getPartialViewContext();
    }

    public TreeTable getTreeTable(String clientId) {
        UIViewRoot view = FacesContext.getCurrentInstance().getViewRoot();
        UIComponent treetable = view.findComponent(clientId);
        return (TreeTable) treetable;
    }

    public static void updateTreeTableColumn(TreeTable table, int index) {
        if (index < 0 || table.getRowCount() < 1
                || index > table.getChildCount()) {
            return;
        }

        int rowCount = (table.getRows() == 0) ? table.getRowCount()
                : table.getRows();

        if (rowCount == 0) {
            return;
        }

        updateTreeTableColumnCells(table, index, rowCount);
    }

    private static void updateTreeTableColumnCells(TreeTable table, int index,
            int rowCount) {
        FacesContext context = FacesContext.getCurrentInstance();
        String tableId = table.getClientId(context);
        char separator = UINamingContainer.getSeparatorChar(context);
        Collection<String> renderIds = getContext().getRenderIds();
        UIColumn column = findColumn(table, index);

        if (column != null) {
            for (UIComponent cell : column.getChildren()) {
                String cellId = cell.getId();

                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    renderIds.add(format("%s%c%d%c%s", tableId, separator,
                            rowIndex, separator, cellId));
                }
            }
        }
    }

    private static UIColumn findColumn(TreeTable table, int index) {
        int columnIndex = 0;

        for (UIComponent column : table.getChildren()) {
            if (column instanceof UIColumn && columnIndex++ == index) {
                return (UIColumn) column;
            }
        }

        return null;
    }

    public static void updateTreeTableRow(TreeTable table, String index) {
        updateTreeTableRow(table, index, false);
    }

    public static void updateTreeTableRow(TreeTable table, String index,
            boolean updateChildren) {
        if (index == null || table.getRowCount() < 1
                || table.getChildCount() == 0) {
            return;
        }

        updateTreeTableRowCells(table, index, updateChildren);
    }

    private static void updateTreeTableRowCells(TreeTable table, String index,
            boolean updateChildren) {
        FacesContext context = FacesContext.getCurrentInstance();
        String tableId = table.getClientId(context);
        char separator = UINamingContainer.getSeparatorChar(context);
        Collection<String> renderIds = getContext().getRenderIds();

        for (UIComponent column : table.getChildren()) {
            if (column instanceof UIColumn) {
                for (UIComponent cell : column.getChildren()) {
                    renderIds.add(format("%s%c%s%c%s", tableId, separator,
                            index, separator, cell.getId()));
                    if (updateChildren) {
                        // TODO Hau rekursibua seme danak prozesetako
                        updateTreeTableRowChildCells(table, table.getRowNode(),
                                tableId, separator, renderIds, cell);
                    }
                }
            }
        }
    }

    private static void updateTreeTableRowChildCells(TreeTable table,
            TreeNode parentNode, String tableId, char separator,
            Collection<String> renderIds, UIComponent cell) {
        for (TreeNode node : parentNode.getChildren()) {
            String rowKey = node.getRowKey();
            renderIds.add(format("%s%c%s%c%s", tableId, separator, rowKey,
                    separator, cell.getId()));

            updateTreeTableRowChildCells(table, node, tableId, separator,
                    renderIds, cell);

        }
    }

    public static void updateDataTableRow(DataTable table, String index) {
        if (index == null || table.getRowCount() < 1) {
            return;
        }

        updateDataTableRowCells(table, index);
    }

    private static void updateDataTableRowCells(DataTable table, String index) {
        FacesContext context = FacesContext.getCurrentInstance();
        String tableId = table.getClientId(context);
        char separator = UINamingContainer.getSeparatorChar(context);
        Collection<String> renderIds = getContext().getRenderIds();

        for (UIComponent column : table.getChildren()) {
            if (column instanceof UIColumn) {
                for (UIComponent cell : column.getChildren()) {
                    renderIds.add(format("%s%c%s%c%s", tableId, separator,
                            index, separator, cell.getId()));
                }
            }
        }
    }

}
