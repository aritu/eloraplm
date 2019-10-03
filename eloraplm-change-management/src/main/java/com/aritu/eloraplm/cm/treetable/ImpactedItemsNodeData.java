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

import com.aritu.eloraplm.cm.ImpactedItem;

/**
 * This class encapsulates the attributes related to an Impacted Item Node Data.
 *
 * @author aritu
 *
 */
public class ImpactedItemsNodeData extends CMItemsNodeData {

    private static final long serialVersionUID = 1L;

    protected DocumentModel modifiedItem;

    public ImpactedItemsNodeData(String id, int level) {

        this(id, level, null, null, null, false, null, null, null, null, null,
                null, false, false, null, false, null, null, false, false,
                false, false, null, null, false, false);
    }

    public ImpactedItemsNodeData(String id, int level, Long rowNumber,
            String nodeId, String parentNodeId, boolean isModifiedItem,
            DocumentModel modifiedItem, DocumentModel parentItem,
            DocumentModel originItem, DocumentModel originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            boolean isDirectObject, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean destinationItemVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated) {

        super(id, level, false, false, false, rowNumber, nodeId, parentNodeId,
                isModifiedItem, parentItem, originItem, originItemWc, predicate,
                quantity, isAnarchic, isDirectObject, action, actionIsReadOnly,
                destinationItem, destinationItemWc,
                destinationItemVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated);

        this.modifiedItem = modifiedItem;
    }

    public ImpactedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, Long rowNumber,
            String nodeId, String parentNodeId, boolean isModifiedItem,
            DocumentModel modifiedItem, DocumentModel parentItem,
            DocumentModel originItem, DocumentModel originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            boolean isDirectObject, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean destinationItemVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated) {

        super(id, level, isNew, isRemoved, isModified, rowNumber, nodeId,
                parentNodeId, isModifiedItem, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, actionIsReadOnly, destinationItem, destinationItemWc,
                destinationItemVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated);

        this.modifiedItem = modifiedItem;
    }

    public ImpactedItem convertToImpactedItem() {
        String modifiedItemUid = null;
        if (getModifiedItem() != null) {
            modifiedItemUid = getModifiedItem().getId();
        }

        String parentItemUid = null;
        if (getParentItem() != null) {
            parentItemUid = getParentItem().getId();
        }

        String originItemUid = null;
        if (getOriginItem() != null) {
            originItemUid = getOriginItem().getId();
        }

        String originItemWcUid = null;
        if (getOriginItemWc() != null) {
            originItemWcUid = getOriginItemWc().getId();
        }

        String destinationItemUid = null;
        if (getDestinationItem() != null) {
            destinationItemUid = getDestinationItem().getId();
        }

        String destinationItemWcUid = null;
        if (getDestinationItemWc() != null) {
            destinationItemWcUid = getDestinationItemWc().getId();
        }

        ImpactedItem impactedItem = new ImpactedItem(getRowNumber(),
                getNodeId(), getParentNodeId(), modifiedItemUid, parentItemUid,
                originItemUid, originItemWcUid, getPredicate(), getQuantity(),
                getIsAnarchic(), getIsDirectObject(), getAction(),
                destinationItemUid, destinationItemWcUid, getIsManaged(),
                getIsManual(), getType(), getComment(), getIsUpdated());

        return impactedItem;
    }

    // Setters and getters
    public DocumentModel getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(DocumentModel modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

}
