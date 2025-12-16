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
package com.aritu.eloraplm.bom.linked.characteristics;

import java.util.Date;

/**
 * This class encapsulates the main attributes of the Bom Characteristic linked
 * to a document.
 *
 * @author aritu
 *
 */
public class LinkedCharacteristicInfo {

    String linkedCharacteristicId;

    String title;

    String type;

    String numberValue;

    String stringValue;

    Date dateValue;

    Boolean booleanValue;

    String listValue;

    String unit;

    Long orderInReport;

    public LinkedCharacteristicInfo(String linkedCharacteristicId, String title,
            String type, String numberValue, String stringValue, Date dateValue,
            Boolean booleanValue, String listValue, String unit,
            Long orderInReport) {
        super();
        this.linkedCharacteristicId = linkedCharacteristicId;
        this.title = title;
        this.type = type;
        this.numberValue = numberValue;
        this.stringValue = stringValue;
        this.dateValue = dateValue;
        this.booleanValue = booleanValue;
        this.listValue = listValue;
        this.unit = unit;
        this.orderInReport = orderInReport;
    }

    public String getLinkedCharacteristicId() {
        return linkedCharacteristicId;
    }

    public void setLinkedCharacteristicId(String linkedCharacteristicId) {
        this.linkedCharacteristicId = linkedCharacteristicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNumberValue() {
        return numberValue;
    }

    public void setNumberValue(String numberValue) {
        this.numberValue = numberValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public String getListValue() {
        return listValue;
    }

    public void setListValue(String listValue) {
        this.listValue = listValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getOrderInReport() {
        return orderInReport;
    }

    public void setOrderInReport(Long orderInReport) {
        this.orderInReport = orderInReport;
    }

}
