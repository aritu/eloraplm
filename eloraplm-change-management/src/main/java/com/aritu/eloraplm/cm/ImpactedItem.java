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
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ImpactedItem {

    long rowNumber;

    String modifiedItem;

    String parentItem;

    String originItem;

    String originItemWc;

    String action;

    String destinationItem;

    boolean isManaged;

    boolean isManual;

    String type;

    String messageType;

    String messageData;

    /**
     * @param rowNumber
     * @param modifiedItem
     * @param parentItem
     * @param originItem
     * @param originItemWc
     * @param action
     * @param destinationItem
     * @param isManaged
     * @param isManual
     * @param type
     * @param messageType
     * @param messageData
     */
    public ImpactedItem(long rowNumber, String modifiedItem, String parentItem,
            String originItem, String originItemWc, String action,
            String destinationItem, boolean isManaged, boolean isManual,
            String type, String messageType, String messageData) {
        super();
        this.rowNumber = rowNumber;
        this.modifiedItem = modifiedItem;
        this.parentItem = parentItem;
        this.originItem = originItem;
        this.originItemWc = originItemWc;
        this.action = action;
        this.destinationItem = destinationItem;
        this.isManaged = isManaged;
        this.isManual = isManual;
        this.type = type;
        this.messageType = messageType;
        this.messageData = messageData;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(String modifiedItem) {
        this.modifiedItem = modifiedItem;
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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageData() {
        return messageData;
    }

    public void setMessageData(String messageData) {
        this.messageData = messageData;
    }

}
