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

public class ModifiedItemsNodeData extends CMItemsNodeData {

    private static final long serialVersionUID = 1L;

    protected DocumentModel derivedFrom;

    protected boolean isImpactable;

    protected boolean includeInImpactMatrix;

    public ModifiedItemsNodeData(String id, int level) {

        this(id, level, null, null, null, null, null, null, null, null, null,
                false, null, false, null, null, false, false, false, false,
                null, null, false, false, false, false);
    }

    public ModifiedItemsNodeData(String id, int level, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String predicate, String quantity,
            boolean isAnarchic, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean destinationItemVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isImpactable, boolean includeInImpactMatrix) {

        this(id, level, false, false, false, rowNumber, nodeId, parentNodeId,
                derivedFrom, parentItem, originItem, originItemWc, predicate,
                quantity, isAnarchic, action, actionIsReadOnly, destinationItem,
                destinationItemWc, destinationItemVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated, isImpactable, includeInImpactMatrix);
    }

    public ModifiedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            DocumentModel parentItem, DocumentModel originItem,
            DocumentModel originItemWc, String predicate, String quantity,
            boolean isAnarchic, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean destinationItemVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isImpactable, boolean includeInImpactMatrix) {

        // In case of modified items, the attributes isModifiedItem is always
        // true.
        super(id, level, isNew, isRemoved, isModified, rowNumber, nodeId,
                parentNodeId, true, parentItem, originItem, originItemWc,
                predicate, quantity, isAnarchic, action, actionIsReadOnly,
                destinationItem, destinationItemWc,
                destinationItemVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated);

        this.derivedFrom = derivedFrom;
        this.isImpactable = isImpactable;
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

    // Setters and getters

    public boolean getIsImpactable() {
        return isImpactable;
    }

    public DocumentModel getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(DocumentModel derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public void setIsImpactable(boolean isImpactable) {
        this.isImpactable = isImpactable;
    }

    public boolean getIncludeInImpactMatrix() {
        return includeInImpactMatrix;
    }

    public void setIncludeInImpactMatrix(boolean includeInImpactMatrix) {
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

}
