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
package com.aritu.eloraplm.cm.util;

/**
 * This class encapsulates the attributes of a Related Item.
 *
 * @author aritu
 *
 */
public class CMImpactNode {

    private String nodeId;

    private String refNodeId;

    private Boolean isDerived;

    public CMImpactNode(String nodeId, Boolean isDerived) {
        this.nodeId = nodeId;
        this.isDerived = isDerived;
        refNodeId = null;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getRefNodeId() {
        return refNodeId;
    }

    public void setRefNodeId(String refNodeId) {
        this.refNodeId = this.refNodeId == null ? refNodeId
                : this.refNodeId + "|" + refNodeId;
    }

    public Boolean getIsDerived() {
        return isDerived;
    }

    public void setIsDerived(Boolean isDerived) {
        this.isDerived = isDerived;
    }

}
