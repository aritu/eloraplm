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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * This class encapsulates the attributes related to an CM Items Node Data.
 *
 * @author aritu
 *
 */
public abstract class CMItemsNodeData extends BaseNodeData {

    private static final long serialVersionUID = 1L;

    protected Long rowNumber;

    protected String nodeId;

    protected String parentNodeId;

    protected boolean isModifiedItem;

    protected DocumentModel parentItem;

    protected DocumentModel originItem;

    protected DocumentModel originItemWc;

    private String predicate;

    private String quantity;

    private boolean isAnarchic;

    protected String action;

    protected boolean actionIsReadOnly;

    protected DocumentModel destinationItem;

    private String destinationItemUid;

    protected DocumentModel destinationItemWc;

    protected Map<String, String> destinationItemVersionList;

    protected boolean destinationItemVersionIsReadOnly;

    protected boolean isManaged;

    protected boolean isManagedIsReadOnly;

    protected boolean isManual;

    protected String type;

    protected String comment;

    protected boolean commentIsReadOnly;

    protected boolean isUpdated;

    public CMItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, Long rowNumber,
            String nodeId, String parentNodeId, boolean isModifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String predicate, String quantity,
            boolean isAnarchic, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean destinationItemVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated) {

        super(id, level, isNew, isRemoved, isModified);
        this.rowNumber = rowNumber;
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        this.isModifiedItem = isModifiedItem;
        this.parentItem = parentItem;
        this.originItem = originItem;
        this.originItemWc = originItemWc;
        this.predicate = predicate;
        this.quantity = quantity;
        this.isAnarchic = isAnarchic;
        this.action = action;
        this.actionIsReadOnly = actionIsReadOnly;
        this.destinationItem = destinationItem;

        // if the destinationItem is the WC, initialize the version list with
        // the WC
        if (destinationItem != null && destinationItemWc != null
                && destinationItem.getId().equals(destinationItemWc.getId())) {
            // initialize the version list with the WC
            destinationItemUid = destinationItem.getId();
            destinationItemVersionList = new HashMap<String, String>();
            destinationItemVersionList.put(destinationItemUid,
                    destinationItem.getVersionLabel() + " (WC)");
        } else {
            destinationItemUid = null;
            destinationItemVersionList = null;
        }
        this.destinationItemWc = destinationItemWc;
        this.destinationItemVersionIsReadOnly = destinationItemVersionIsReadOnly;
        this.isManaged = isManaged;
        this.isManagedIsReadOnly = isManagedIsReadOnly;
        this.isManual = isManual;
        this.type = type;
        this.comment = comment;
        this.commentIsReadOnly = commentIsReadOnly;
        this.isUpdated = isUpdated;
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

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public void setIsModifiedItem(boolean isModifiedItem) {
        this.isModifiedItem = isModifiedItem;
    }

    public boolean getIsModifiedItem() {
        return isModifiedItem;
    }

    public DocumentModel getParentItem() {
        return parentItem;
    }

    public void setParentItem(DocumentModel parentItem) {
        this.parentItem = parentItem;
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

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public boolean getIsAnarchic() {
        return isAnarchic;
    }

    public void setIsAnarchic(boolean isAnarchic) {
        this.isAnarchic = isAnarchic;
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

    public DocumentModel getDestinationItem() {
        return destinationItem;
    }

    public String getDestinationItemUid() {
        return destinationItemUid;
    }

    public void setDestinationItemUid(String destinationItemUid) {
        this.destinationItemUid = destinationItemUid;
    }

    public void setDestinationItem(DocumentModel destinationItem) {
        this.destinationItem = destinationItem;
    }

    public DocumentModel getDestinationItemWc() {
        return destinationItemWc;
    }

    public void setDestinationItemWc(DocumentModel destinationItemWc) {
        this.destinationItemWc = destinationItemWc;
    }

    public Map<String, String> getDestinationItemVersionList() {
        return destinationItemVersionList;
    }

    public void setDestinationItemVersionList(
            Map<String, String> destinationItemVersionList) {
        this.destinationItemVersionList = destinationItemVersionList;
    }

    public boolean getDestinationItemVersionIsReadOnly() {
        return destinationItemVersionIsReadOnly;
    }

    public void setDestinationItemVersionIsReadOnly(
            boolean destinationItemVersionIsReadOnly) {
        this.destinationItemVersionIsReadOnly = destinationItemVersionIsReadOnly;
    }

    public boolean getIsManaged() {
        return isManaged;
    }

    public void setIsManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public boolean getIsManagedIsReadOnly() {
        return isManagedIsReadOnly;
    }

    public void setIsManagedIsReadOnly(boolean isManagedIsReadOnly) {
        this.isManagedIsReadOnly = isManagedIsReadOnly;
    }

    public boolean getIsManual() {
        return isManual;
    }

    public void setIsManual(boolean isManual) {
        this.isManual = isManual;
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

    public boolean isCommentIsReadOnly() {
        return commentIsReadOnly;
    }

    public void setCommentIsReadOnly(boolean commentIsReadOnly) {
        this.commentIsReadOnly = commentIsReadOnly;
    }

    public boolean getIsUpdated() {
        return isUpdated;
    }

    public void setIsUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

}
