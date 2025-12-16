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
package com.aritu.eloraplm.cm;

/**
 * This class encapsulates the attributes related to Modified Item.
 *
 * @author aritu
 *
 */
public class ImpactedItem extends CMItem {

    protected String modifiedItem;

    protected String refNodeId;

    protected boolean isDerived;

    protected boolean isFromMbom;

    protected String externalReference;

    protected String externalName;

    public ImpactedItem(Long rowNumber, String nodeId, String parentNodeId,
            String derivedFrom, String modifiedItem, String parentItem,
            String originItem, String originItemWc, String predicate,
            String quantity, boolean isAnarchic, boolean isDirectObject,
            String action, String destinationItem, String destinationItemWc,
            String destinationItemSourceVersion, boolean isManaged,
            boolean isManual, String type, String comment, boolean isUpdated,
            boolean isDerived, String refNodeId) {

        this(rowNumber, nodeId, parentNodeId, derivedFrom, modifiedItem,
                parentItem, originItem, originItemWc, predicate, quantity,
                isAnarchic, isDirectObject, action, destinationItem,
                destinationItemWc, destinationItemSourceVersion, isManaged,
                isManual, type, comment, isUpdated, isDerived, refNodeId, false,
                null, null);
    }

    public ImpactedItem(Long rowNumber, String nodeId, String parentNodeId,
            String derivedFrom, String modifiedItem, String parentItem,
            String originItem, String originItemWc, String predicate,
            String quantity, boolean isAnarchic, boolean isDirectObject,
            String action, String destinationItem, String destinationItemWc,
            String destinationItemSourceVersion, boolean isManaged,
            boolean isManual, String type, String comment, boolean isUpdated,
            boolean isDerived, String refNodeId, boolean isFromMbom,
            String externalReference, String externalName) {

        super(rowNumber, nodeId, parentNodeId, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, destinationItem, destinationItemWc,
                destinationItemSourceVersion, isManaged, isManual, type,
                comment, isUpdated, derivedFrom);

        this.modifiedItem = modifiedItem;
        this.isDerived = isDerived;
        this.refNodeId = refNodeId;
        this.isFromMbom = isFromMbom;
        this.externalReference = externalReference;
        this.externalName = externalName;
        this.destinationItemSourceVersion = destinationItemSourceVersion;
    }

    public String getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(String modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

    public boolean isDerived() {
        return isDerived;
    }

    public void setDerived(boolean isDerived) {
        this.isDerived = isDerived;
    }

    public String getRefNodeId() {
        return refNodeId;
    }

    public void setRefNodeId(String refNodeId) {
        this.refNodeId = refNodeId;
    }

    public boolean getIsFromMbom() {
        return isFromMbom;
    }

    public void setIsFromMbom(boolean isFromMbom) {
        this.isFromMbom = isFromMbom;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

}
