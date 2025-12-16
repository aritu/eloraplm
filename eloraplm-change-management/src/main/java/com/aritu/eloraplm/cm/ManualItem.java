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
public class ManualItem {

    protected Long rowNumber;

    protected String nodeId;

    protected String modifiedItem;

    protected String originItem;

    protected String action;

    protected String type;

    protected String comment;

    public ManualItem(Long rowNumber, String nodeId, String modifiedItem,
            String originItem, String action, String type, String comment) {
        this.nodeId = nodeId;
        this.modifiedItem = modifiedItem;
        this.originItem = originItem;
        this.action = action;
        this.type = type;
        this.comment = comment;
        this.rowNumber = rowNumber;
    }

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

    public String getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(String modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

    public String getOriginItem() {
        return originItem;
    }

    public void setOriginItem(String originItem) {
        this.originItem = originItem;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

}
