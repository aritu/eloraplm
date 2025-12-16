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

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.relations.treetable.BomListExternalData;

/**
 * This class encapsulates the attributes related to an Impacted Item Node Data.
 *
 * @author aritu
 *
 */
public class ImpactedItemsNodeData extends CMItemsNodeData {

    private static final long serialVersionUID = 1L;

    protected DocumentModel modifiedItem;

    protected boolean isDerived;

    protected String refNodeId;

    protected boolean isFromMbom;

    protected boolean isCharacterized;

    protected BomListExternalData bomListExternalData;

    public ImpactedItemsNodeData(String id, int level) {

        this(id, level, null, null, null, null, false, null, null, null, null,
                null, null, false, false, null, false, null, null, null, null,
                false, false, false, false, false, null, null, false, false,
                false, null, false, false, null, null);
    }

    public ImpactedItemsNodeData(String id, int level, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            boolean isModifiedItem, DocumentModel modifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String action,
            boolean actionIsReadOnly, DocumentModel destinationItem,
            DocumentModel destinationItemWc,
            String defaultDestinationItemSourceVersionUid,
            DocumentModel destinationItemSourceVersion,
            boolean destinationItemVersionIsReadOnly,
            boolean destinationItemSourceVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isDerived, String refNodeId, Map<String, String> messages) {

        this(id, level, rowNumber, nodeId, parentNodeId, derivedFrom,
                isModifiedItem, modifiedItem, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, actionIsReadOnly, destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, destinationItemVersionIsReadOnly,
                destinationItemSourceVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated, isDerived, refNodeId, false, false, null, messages);
    }

    public ImpactedItemsNodeData(String id, int level, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            boolean isModifiedItem, DocumentModel modifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String action,
            boolean actionIsReadOnly, DocumentModel destinationItem,
            DocumentModel destinationItemWc,
            String defaultDestinationItemSourceVersionUid,
            DocumentModel destinationItemSourceVersion,
            boolean destinationItemVersionIsReadOnly,
            boolean destinationItemSourceVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isDerived, String refNodeId, boolean isFromMbom,
            boolean isCharacterized, BomListExternalData bomListExternalData,
            Map<String, String> messages) {

        super(id, level, false, false, false, rowNumber, nodeId, parentNodeId,
                derivedFrom, isModifiedItem, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, actionIsReadOnly, destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, destinationItemVersionIsReadOnly,
                destinationItemSourceVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated, messages);

        this.modifiedItem = modifiedItem;
        this.isDerived = isDerived;
        this.refNodeId = refNodeId;
        this.isFromMbom = isFromMbom;
        this.isCharacterized = isCharacterized;
        this.bomListExternalData = bomListExternalData;
    }

    public ImpactedItem convertToImpactedItem() {
        String modifiedItemUid = null;
        if (getModifiedItem() != null) {
            modifiedItemUid = getModifiedItem().getId();
        }

        String refNodeId = null;
        if (getRefNodeId() != null) {
            refNodeId = getRefNodeId();
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

        String destinationItemSourceVersionUid = null;
        if (getDestinationItemSourceVersion() != null) {
            destinationItemSourceVersionUid = getDestinationItemSourceVersion().getId();
        }

        String derivedFromUid = null;
        if (getDerivedFrom() != null) {
            derivedFromUid = getDerivedFrom().getId();
        }

        String externalReference = null;
        String externalName = null;
        if (getBomListExternalData() != null) {
            externalReference = getBomListExternalData().getReference();
            externalName = getBomListExternalData().getName();
        }

        ImpactedItem impactedItem = new ImpactedItem(getRowNumber(),
                getNodeId(), getParentNodeId(), derivedFromUid, modifiedItemUid,
                parentItemUid, originItemUid, originItemWcUid, getPredicate(),
                getQuantity(), getIsAnarchic(), getIsDirectObject(),
                getAction(), destinationItemUid, destinationItemWcUid,
                destinationItemSourceVersionUid, getIsManaged(), getIsManual(),
                getType(), getComment(), getIsUpdated(), getIsDerived(),
                refNodeId, getIsFromMbom(), externalReference, externalName);

        return impactedItem;
    }

    // Setters and getters
    public DocumentModel getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(DocumentModel modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

    public String getRefNodeId() {
        return refNodeId;
    }

    public void setRefNodeId(String refNodeId) {
        this.refNodeId = refNodeId;
    }

    public boolean getIsDerived() {
        return isDerived;
    }

    public boolean getIsFromMbom() {
        return isFromMbom;
    }

    public void setIsFromMbom(boolean isFromMbom) {
        this.isFromMbom = isFromMbom;
    }

    public BomListExternalData getBomListExternalData() {
        return bomListExternalData;
    }

    public void setBomListExternalData(
            BomListExternalData bomListExternalData) {
        this.bomListExternalData = bomListExternalData;
    }

    public boolean getIsCharacterized() {
        return isCharacterized;
    }

    public void setIsCharacterized(boolean isCharacterized) {
        this.isCharacterized = isCharacterized;
    }

}
