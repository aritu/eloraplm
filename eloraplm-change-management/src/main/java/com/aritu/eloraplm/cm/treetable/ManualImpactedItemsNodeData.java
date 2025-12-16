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
package com.aritu.eloraplm.cm.treetable;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * This class encapsulates the attributes related to an CM Items Node Data.
 *
 * @author aritu
 *
 */
public class ManualImpactedItemsNodeData extends BaseNodeData {

    private static final long serialVersionUID = 1L;

    protected Long rowNumber;

    protected String nodeId;

    protected String modifiedItemId;

    protected boolean isModifiedItem;

    protected DocumentModel originItem;

    protected DocumentModel originItemWc;

    protected String action;

    protected boolean actionIsReadOnly;

    protected String type;

    protected String comment;

    public ManualImpactedItemsNodeData(String id, int level) {
        this(id, level, false, false, false, null, null, null, false, null,
                null, null, false, null, null);
    }

    public ManualImpactedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, Long rowNumber,
            String nodeId, String modifiedItemId, boolean isModifiedItem,
            DocumentModel originItem, DocumentModel originItemWc, String action,
            boolean actionIsReadOnly, String type, String comment) {

        super(id, level, isNew, isRemoved, isModified);
        this.rowNumber = rowNumber;
        this.nodeId = nodeId;
        this.modifiedItemId = modifiedItemId;
        this.isModifiedItem = isModifiedItem;
        this.originItem = originItem;
        this.originItemWc = originItemWc;
        this.action = action;
        this.actionIsReadOnly = actionIsReadOnly;
        this.type = type;
        this.comment = comment;
    }

    // Setters and getters
    public Long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getModifiedItemId() {
        return modifiedItemId;
    }

    public void setModifiedItemId(String modifiedItemId) {
        this.modifiedItemId = modifiedItemId;
    }

    public boolean getIsModifiedItem() {
        return isModifiedItem;
    }

    public void setIsModifiedItem(boolean isModifiedItem) {
        this.isModifiedItem = isModifiedItem;
    }

    public DocumentModel getOriginItem() {
        return originItem;
    }

    public void setOriginItem(DocumentModel originItem) {
        this.originItem = originItem;
    }

    public DocumentModel getOriginItemWc() {
        return originItemWc;
    }

    public void setOriginItemWc(DocumentModel originItemWc) {
        this.originItemWc = originItemWc;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean getActionIsReadOnly() {
        return actionIsReadOnly;
    }

    public void setActionIsReadOnly(boolean actionIsReadOnly) {
        this.actionIsReadOnly = actionIsReadOnly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
