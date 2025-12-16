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

/**
 * @author aritu
 *
 */

public class ModifiedItemsNodeData extends CMItemsNodeData {

    private static final long serialVersionUID = 1L;

    protected boolean isDerivedFromImpactMatrix;

    protected boolean isImpactable;

    protected boolean includeInImpactMatrix;

    public ModifiedItemsNodeData(String id, int level) {

        this(id, level, null, null, null, null, false, null, null, null, null,
                null, false, false, null, false, null, null, null, null, false,
                false, false, false, false, null, null, false, false, false,
                false, null);
    }

    public ModifiedItemsNodeData(String id, int level, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            boolean isDerivedFromImpactMatrix, DocumentModel parentItem,
            DocumentModel originItem, DocumentModel originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            boolean isDirectObject, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            String defaultDestinationItemSourceVersionUid,
            DocumentModel destinationItemSourceVersion,
            boolean destinationItemVersionIsReadOnly,
            boolean destinationItemSourceVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isImpactable, boolean includeInImpactMatrix,
            Map<String, String> messages) {

        this(id, level, false, false, false, rowNumber, nodeId, parentNodeId,
                derivedFrom, isDerivedFromImpactMatrix, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, actionIsReadOnly, destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, destinationItemVersionIsReadOnly,
                destinationItemSourceVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated, isImpactable, includeInImpactMatrix, messages);
    }

    public ModifiedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, Long rowNumber,
            String nodeId, String parentNodeId, DocumentModel derivedFrom,
            boolean isDerivedFromImpactMatrix, DocumentModel parentItem,
            DocumentModel originItem, DocumentModel originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            boolean isDirectObject, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            String defaultDestinationItemSourceVersionUid,
            DocumentModel destinationItemSourceVersion,
            boolean destinationItemVersionIsReadOnly,
            boolean destinationItemSourceVersionIsReadOnly, boolean isManaged,
            boolean isManagedIsReadOnly, boolean isManual, String type,
            String comment, boolean commentIsReadOnly, boolean isUpdated,
            boolean isImpactable, boolean includeInImpactMatrix,
            Map<String, String> messages) {

        // In case of modified items, the attributes isModifiedItem is always
        // true.
        super(id, level, isNew, isRemoved, isModified, rowNumber, nodeId,
                parentNodeId, derivedFrom, true, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, actionIsReadOnly, destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, destinationItemVersionIsReadOnly,
                destinationItemSourceVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, isManual, type, comment, commentIsReadOnly,
                isUpdated, messages);

        this.isDerivedFromImpactMatrix = isDerivedFromImpactMatrix;
        this.isImpactable = isImpactable;
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

    // Setters and getters

    public boolean getIsImpactable() {
        return isImpactable;
    }

    public boolean getIsDerivedFromImpactMatrix() {
        return isDerivedFromImpactMatrix;
    }

    public void setIsDerivedFromImpactMatrix(
            boolean isDerivedFromImpactMatrix) {
        this.isDerivedFromImpactMatrix = isDerivedFromImpactMatrix;
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
