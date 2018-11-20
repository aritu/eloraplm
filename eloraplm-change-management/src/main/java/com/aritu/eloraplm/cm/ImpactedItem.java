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

    public ImpactedItem(Long rowNumber, String nodeId, String parentNodeId,
            String modifiedItem, String parentItem, String originItem,
            String originItemWc, String predicate, String quantity,
            boolean isAnarchic, String action, String destinationItem,
            String destinationItemWc, boolean isManaged, boolean isManual,
            String type, String comment, boolean isUpdated) {
        super(rowNumber, nodeId, parentNodeId, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, action,
                destinationItem, destinationItemWc, isManaged, isManual, type,
                comment, isUpdated);

        this.modifiedItem = modifiedItem;
    }

    public String getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(String modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

}
