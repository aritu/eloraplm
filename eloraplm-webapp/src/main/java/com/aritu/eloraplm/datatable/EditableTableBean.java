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
package com.aritu.eloraplm.datatable;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.RowEditEvent;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.webapp.util.EloraAjax;

/**
 *
 * @author aritu
 *
 */
public abstract class EditableTableBean extends CoreTableBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EditableTableBean.class);

    private Date currentLastModified;

    @In(create = true)
    protected transient WebActions webActions;

    private String rowId;

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public EditableTableBean() {
    }

    /* Treetableetik hartuta, ondo begiratu gabe */
    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding row |" + rowId
                + "| to datatable of document |" + getCurrentDocument().getId()
                + "|.");

        try {
            // TODO Txekeo honeik ondo dauz???
            // if ((isAnarchic && (!currentDoc.isLocked()
            // || currentDoc.getLockInfo().getOwner().equals(
            // documentManager.getPrincipal().getName())))
            // || (currentDoc.isLocked()
            // && currentDoc.getLockInfo().getOwner().equals(
            // documentManager.getPrincipal().getName()))) {
            // TODO txekeo gehixau??
            // DocumentModel objectDoc = documentManager.getDocument(
            // new IdRef(objectDocumentUid));

            // TODO Inverse erlazioetan ez dabil, eta Save egin gabe badago
            // beste nodo bat (benetako erlazioak sortu gabe), bere azpiko
            // dokumentuak ere ez ditu txekeatzen. Baina kasu normaletarako
            // balio du.
            // if (!EloraRelationHelper.isCircularRelation(currentDoc,
            // objectDoc, documentManager)) {
            // boolean objectHasVersion =
            // documentManager.getLastDocumentVersion(
            // objectDoc.getRef()) != null;
            // boolean docHasVersion = documentManager.getLastDocumentVersion(
            // currentDoc.getRef()) != null;

            // if (!isAnarchic || (docHasVersion && objectHasVersion)) {
            // String nextNodeId = Integer.toString(
            // getRoot().getChildCount() + 1);

            // DocumentModel wcDoc = null;
            // if (objectDoc.isImmutable()) {
            // wcDoc = documentManager.getWorkingCopy(
            // objectDoc.getRef());
            // } else {
            // wcDoc = objectDoc;
            // }

            // Add a new EditableRelationNode
            // Some values are not real because we will need and
            // calculate them after we save the tree
            RowData row = createAndAddRowData(rowId, true, false);

            log.trace(logInitMsg + "Row |" + rowId + "| added.");

            resetCreateFormValues();
            setIsDirty(true);

            /* ZETAKO DA HAU??????
            // When we reset tabs, the current tab/subtab is lost,
            // so we have to get it before, and reset it after
            String currentTabId = webActions.getCurrentTabId();
            String currentSubTabId = webActions.getCurrentSubTabId();
            webActions.resetTabList();
            webActions.setCurrentTabId(currentTabId);
            webActions.setCurrentSubTabId(currentSubTabId);
            */

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.datatable.row.added"));
            //
            // } else {
            // log.error(
            // logInitMsg + "The object document has no AVs.");
            // facesMessages.add(StatusMessage.Severity.WARN,
            // messages.get(
            // "eloraplm.message.error.documentWithoutVersion"));
            // }
            // } else {
            // log.error(logInitMsg
            // + "Adding this object document will cause a circular relation.");
            // facesMessages.add(StatusMessage.Severity.WARN, messages.get(
            // "eloraplm.message.error.circularRelation"));
            // }
            // } else if (!currentDoc.isLocked()) {
            // log.error(logInitMsg + "The subject document is not locked.");
            // facesMessages.add(StatusMessage.Severity.ERROR,
            // messages.get("label.relation.documentNotLocked"));
            // } else {
            // log.error(logInitMsg
            // + "The subject document is locked by another user.");
            // facesMessages.add(StatusMessage.Severity.ERROR,
            // messages.get("label.relation.documentLockedByOther"));
            // }
        } catch (

        Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.addRelationNode"));
        }
    }

    protected RowData createAndAddRowData(String rowId, boolean isNew,
            boolean isRemoved) {
        RowData row = tableService.createRowData(rowId, isNew, false,
                isRemoved);
        getData().add(row);

        return row;
    }

    public void markRowAsModified(DataTable table, RowData row,
            String rowIndex) {
        row.setIsModified(true);
        setIsDirty(true);
        // HAU BERDIN???
        // EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    public void markRowAsRemoved(DataTable table, RowData row,
            String rowIndex) {
        toggleRowAsRemoved(table, row, rowIndex, true);
    }

    public void unmarkRowAsRemoved(DataTable table, RowData row,
            String rowIndex) {
        toggleRowAsRemoved(table, row, rowIndex, false);

    }

    private void toggleRowAsRemoved(DataTable table, RowData row,
            String rowIndex, boolean isRemoved) {
        if (isRemoved && row.getIsNew()) {
            removeRow(row.getId());
        }

        row.setIsRemoved(isRemoved);
        setIsDirty(true);
        EloraAjax.updateDataTableRow(table, rowIndex);
    }
    /* END Treetableetik hartuta, ondo begiratu gabe */

    protected abstract void save();

    /* Treetableetik hartuta, ondo begiratu gabe */

    public void refreshRow(DataTable table, String rowIndex) {
        EloraAjax.updateDataTableRow(table, rowIndex);
    }

    private void refreshRow(RowData row) {
        // String logInitMsg = "[refreshNode] ["
        // + documentManager.getPrincipal().getName() + "] ";
        //
        // RelationNodeData nodeData = (RelationNodeData) node.getData();
        // String oldDocId = nodeData.getData().getId();
        // if (!nodeData.getDocId().equals(oldDocId)) {
        // DocumentModel newDoc = documentManager.getDocument(
        // new IdRef(nodeData.getDocId()));
        // nodeData.setData(newDoc);
        // node.getChildren().clear();
        //
        // log.trace(logInitMsg + "Refreshed node from |" + oldDocId + "| to |"
        // + newDoc.getId() + "|.");
        // }
        // nodeData.setIsNew(false);
        // nodeData.setIsModified(true);
        //
        // setIsDirty(true);
    }

    /* END Treetableetik hartuta, ondo begiratu gabe */

    protected void resetCreateFormValues() {
        rowId = null;
    }

    /* Treetableetik hartuta, ondo begiratu gabe */
    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        Date newLastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            // Taken from Nuxeo code:
            // -------------------------------------------------
            // remove milliseconds as they are not stored in some
            // databases, which could make the comparison fail just
            // after a document creation (see NXP-8783)
            // -------------------------------------------------
            lastModifiedGc.set(Calendar.MILLISECOND, 0);
            newLastModified = lastModifiedGc.getTime();
        }

        boolean invalidate = false;
        if (currentLastModified == null || newLastModified == null) {
            if (!(currentLastModified == null && newLastModified == null)) {
                invalidate = true;
            }
        } else {
            if (currentLastModified.compareTo(newLastModified) != 0) {
                invalidate = true;
            }
        }

        if (invalidate) {
            currentLastModified = newLastModified;
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different modification date. Current: |"
                    + currentLastModified + "| New: |" + newLastModified + "|");
        }

        return doc;
    }

    /* END Treetableetik hartuta, ondo begiratu gabe */

    public void onRowEdit(RowEditEvent event) {
        RowData row = (RowData) event.getObject();
        row.setIsModified(true);
        setIsDirty(true);
    }
}
