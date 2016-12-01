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

/**
 * @author aritu
 *
 */
/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CmImpactedItemsNodeData extends CmItemsNodeData {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private long rowNumber;

    private DocumentModel modifiedItem;

    private DocumentModel parentItem;

    private boolean isManual;

    private String messageType;

    private String messageData;

    public CmImpactedItemsNodeData(String id, int level) {

        this(id, level, false, false, false, 0, false, null, null, null, null,
                null, false, null, false, false, false, null, null, null);
    }

    public CmImpactedItemsNodeData(String id, int level, long rowNumber,
            boolean isModifiedItem, DocumentModel modifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String messageType, String messageData) {

        this(id, level, false, false, false, rowNumber, isModifiedItem,
                modifiedItem, parentItem, originItem, originItemWc, action,
                actionIsReadOnly, destinationItem, isManaged,
                isManagedIsReadOnly, isManual, type, messageType, messageData);
    }

    public CmImpactedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, long rowNumber,
            boolean isModifiedItem, DocumentModel modifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String messageType, String messageData) {

        super(id, level, isNew, isRemoved, isModified, originItem, originItemWc,
                action, actionIsReadOnly, destinationItem, isManaged,
                isManagedIsReadOnly, type);

        this.rowNumber = rowNumber;
        this.isModifiedItem = isModifiedItem;
        this.modifiedItem = modifiedItem;
        this.parentItem = parentItem;
        this.isManual = isManual;
        this.messageType = messageType;
        this.messageData = messageData;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public DocumentModel getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(DocumentModel modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

    public DocumentModel getParentItem() {
        return parentItem;
    }

    public void setParentItem(DocumentModel parentItem) {
        this.parentItem = parentItem;
    }

    public boolean getIsManual() {
        return isManual;
    }

    public void setIsManual(boolean isManual) {
        this.isManual = isManual;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageData() {
        return messageData;
    }

    public void setMessageData(String messageData) {
        this.messageData = messageData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (isManual ? 1231 : 1237);
        result = prime * result
                + ((messageData == null) ? 0 : messageData.hashCode());
        result = prime * result
                + ((messageType == null) ? 0 : messageType.hashCode());
        result = prime * result
                + ((modifiedItem == null) ? 0 : modifiedItem.hashCode());
        result = prime * result
                + ((parentItem == null) ? 0 : parentItem.hashCode());
        result = prime * result + (int) (rowNumber ^ (rowNumber >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CmImpactedItemsNodeData other = (CmImpactedItemsNodeData) obj;
        if (isManual != other.isManual) {
            return false;
        }
        if (messageData == null) {
            if (other.messageData != null) {
                return false;
            }
        } else if (!messageData.equals(other.messageData)) {
            return false;
        }
        if (messageType == null) {
            if (other.messageType != null) {
                return false;
            }
        } else if (!messageType.equals(other.messageType)) {
            return false;
        }
        if (modifiedItem == null) {
            if (other.modifiedItem != null) {
                return false;
            }
        } else if (!modifiedItem.equals(other.modifiedItem)) {
            return false;
        }
        if (parentItem == null) {
            if (other.parentItem != null) {
                return false;
            }
        } else if (!parentItem.equals(other.parentItem)) {
            return false;
        }
        if (rowNumber != other.rowNumber) {
            return false;
        }
        return true;
    }

    // TODO:: hau beharrezkoa da???
    @Override
    public String toString() {
        return "CmImpactedItemsNodeData [isModifiedItem=" + isModifiedItem
                + ", rowNumber=" + rowNumber + ", modifiedItem=" + modifiedItem
                + ", parentItem=" + parentItem + ", originItem=" + originItem
                + ", originItemWc=" + originItemWc + ", action=" + action
                + ", actionIsReadOnly=" + actionIsReadOnly
                + ", destinationItem=" + destinationItem + ", isManaged="
                + isManaged + ", isManagedIsReadOnly=" + isManagedIsReadOnly
                + ", isManual=" + isManual + ", type=" + type + ", messageType="
                + messageType + ", messageData=" + messageData + "]";
    }

    // TODO:: hau beharrezkoa da???
    @Override
    public int compareTo(Object obj) {
        CmImpactedItemsNodeData objNode = (CmImpactedItemsNodeData) obj;

        String itemUniqueId = getOriginItem().getId();

        String objItemUniqueId = objNode.getOriginItem().getId();

        return itemUniqueId.compareTo(objItemUniqueId);
    }

}
