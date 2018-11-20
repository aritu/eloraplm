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
public class ModifiedItem extends CMItem {

    protected String derivedFrom;

    protected boolean includeInImpactMatrix;

    /*public ModifiedItem(String nodeId, String parentNodeId, String derivedFrom,
            String parentItem, String originItem, String originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            String action, String destinationItem, String destinationItemWc,
            boolean isManaged, boolean isManual, String type, String comment,
            boolean isUpdated, boolean includeInImpactMatrix) {
        super(nodeId, parentNodeId, parentItem, originItem, originItemWc,
                predicate, quantity, isAnarchic, action, destinationItem,
                destinationItemWc, isManaged, isManual, type, comment,
                isUpdated);
        this.derivedFrom = derivedFrom;
        this.includeInImpactMatrix = includeInImpactMatrix;
    }*/

    public ModifiedItem(Long rowNumber, String nodeId, String parentNodeId,
            String derivedFrom, String parentItem, String originItem,
            String originItemWc, String predicate, String quantity,
            boolean isAnarchic, String action, String destinationItem,
            String destinationItemWc, boolean isManaged, boolean isManual,
            String type, String comment, boolean isUpdated,
            boolean includeInImpactMatrix) {
        super(rowNumber, nodeId, parentNodeId, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, action,
                destinationItem, destinationItemWc, isManaged, isManual, type,
                comment, isUpdated);
        this.derivedFrom = derivedFrom;
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

    public String getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(String derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public boolean getIncludeInImpactMatrix() {
        return includeInImpactMatrix;
    }

    public void setIncludeInImpactMatrix(boolean includeInImpactMatrix) {
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

}
