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
 * This class encapsulates the fields that are common for Modified and Impacted
 * items.
 *
 * @author aritu
 *
 */
public abstract class CMItem {

    protected Long rowNumber;

    protected String nodeId;

    protected String parentNodeId;

    protected String parentItem;

    protected String originItem;

    protected String originItemWc;

    protected String predicate;

    protected String quantity;

    protected boolean isAnarchic;

    protected boolean isDirectObject;

    protected String action;

    protected String destinationItem;

    protected String destinationItemWc;

    protected boolean isManaged;

    protected boolean isManual;

    protected String type;

    protected String comment;

    protected boolean isUpdated;

    /**
     * @param rowNumber
     * @param nodeId
     * @param parentNodeId
     * @param parentItem
     * @param originItem
     * @param originItemWc
     * @param predicate
     * @param quantity
     * @param isAnarchic
     * @param isDirectObject
     * @param action
     * @param destinationItem
     * @param destinationItemWc
     * @param isManaged
     * @param isManual
     * @param type
     * @param comment
     * @param isUpdated
     */
    public CMItem(Long rowNumber, String nodeId, String parentNodeId,
            String parentItem, String originItem, String originItemWc,
            String predicate, String quantity, boolean isAnarchic,
            boolean isDirectObject, String action, String destinationItem,
            String destinationItemWc, boolean isManaged, boolean isManual,
            String type, String comment, boolean isUpdated) {
        super();
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        this.parentItem = parentItem;
        this.originItem = originItem;
        this.originItemWc = originItemWc;
        this.predicate = predicate;
        this.quantity = quantity;
        this.isAnarchic = isAnarchic;
        this.isDirectObject = isDirectObject;
        this.action = action;
        this.destinationItem = destinationItem;
        this.destinationItemWc = destinationItemWc;
        this.isManaged = isManaged;
        this.isManual = isManual;
        this.type = type;
        this.comment = comment;
        this.isUpdated = isUpdated;
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

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public String getParentItem() {
        return parentItem;
    }

    public void setParentItem(String parentItem) {
        this.parentItem = parentItem;
    }

    public String getOriginItem() {
        return originItem;
    }

    public void setOriginItem(String originItem) {
        this.originItem = originItem;
    }

    public String getOriginItemWc() {
        return originItemWc;
    }

    public void setOriginItemWc(String originItemWc) {
        this.originItemWc = originItemWc;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public boolean isAnarchic() {
        return isAnarchic;
    }

    public void setAnarchic(boolean isAnarchic) {
        this.isAnarchic = isAnarchic;
    }

    public boolean isDirectObject() {
        return isDirectObject;
    }

    public void setDirectObject(boolean isDirectObject) {
        this.isDirectObject = isDirectObject;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDestinationItem() {
        return destinationItem;
    }

    public void setDestinationItem(String destinationItem) {
        this.destinationItem = destinationItem;
    }

    public String getDestinationItemWc() {
        return destinationItemWc;
    }

    public void setDestinationItemWc(String destinationItemWc) {
        this.destinationItemWc = destinationItemWc;
    }

    public boolean isManaged() {
        return isManaged;
    }

    public void setManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public boolean isManual() {
        return isManual;
    }

    public void setManual(boolean isManual) {
        this.isManual = isManual;
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

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

}
