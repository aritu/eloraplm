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
package com.aritu.eloraplm.bom.characteristics;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates the attributes related to a Bom Characteristic
 *
 * @author aritu
 *
 */
public class BomCharacteristic {

    String bomCharacteristicId;

    String classification;

    // String identifying the DocumentModel of the BOM Characteristic Master
    String bomCharacteristicMaster;

    // last modified date of the BomCharacteristicMaster related to this
    // BomCharacteristic
    Date bomCharacteristicMasterLastModified;

    Long order;

    String title;

    String description;

    String type;

    Long numberMaxIntegerPlaces;

    Long numberMaxDecimalPlaces;

    String numberDefaultValue;

    String numberValue;

    Long stringMaxLength;

    String stringDefaultValue;

    String stringValue;

    Date dateDefaultValue;

    Date dateValue;

    Boolean booleanDefaultValue;

    Boolean booleanValue;

    List<Map<String, String>> listContent;

    String listDefaultValue;

    String listValue;

    String unit;

    Boolean showInReport;

    Long orderInReport;

    Boolean required;

    Boolean includeInTitle;

    Boolean unmodifiable;

    String messageType;

    String message;

    /**
    *
    */
    public BomCharacteristic(String bomCharacteristicId,
            String bomCharacteristicMaster,
            Date bomCharacteristicMasterLastModified) {
        super();
        this.bomCharacteristicId = bomCharacteristicId;
        this.bomCharacteristicMaster = bomCharacteristicMaster;
        this.bomCharacteristicMasterLastModified = bomCharacteristicMasterLastModified;
    }

    /**
     * @param bomCharacteristicId
     * @param classification
     * @param bomCharacteristicMaster
     * @param bomCharacteristicMasterLastModified
     * @param order
     * @param title
     * @param description
     * @param type
     * @param numberMaxIntegerPlaces
     * @param numberMaxDecimalPlaces
     * @param numberDefaultValue
     * @param numberValue
     * @param stringMaxLength
     * @param stringDefaultValue
     * @param stringValue
     * @param dateDefaultValue
     * @param dateValue
     * @param booleanDefaultValue
     * @param booleanValue
     * @param listContent
     * @param listDefaultValue
     * @param listValue
     * @param unit
     * @param showInReport
     * @param orderInReport
     * @param required
     * @param includeInTitle
     * @param unmodifiable
     */
    public BomCharacteristic(String bomCharacteristicId, String classification,
            String bomCharacteristicMaster,
            Date bomCharacteristicMasterLastModified, Long order, String title,
            String description, String type, Long numberMaxIntegerPlaces,
            Long numberMaxDecimalPlaces, String numberDefaultValue,
            String numberValue, Long stringMaxLength, String stringDefaultValue,
            String stringValue, Date dateDefaultValue, Date dateValue,
            Boolean booleanDefaultValue, Boolean booleanValue,
            List<Map<String, String>> listContent, String listDefaultValue,
            String listValue, String unit, Boolean showInReport,
            Long orderInReport, Boolean required, Boolean includeInTitle,
            Boolean unmodifiable) {
        this(bomCharacteristicId, classification, bomCharacteristicMaster,
                bomCharacteristicMasterLastModified, order, title, description,
                type, numberMaxIntegerPlaces, numberMaxDecimalPlaces,
                numberDefaultValue, numberValue, stringMaxLength,
                stringDefaultValue, stringValue, dateDefaultValue, dateValue,
                booleanDefaultValue, booleanValue, listContent,
                listDefaultValue, listValue, unit, showInReport, orderInReport,
                required, includeInTitle, unmodifiable, null, null);
    }

    /**
     * @param bomCharacteristicId
     * @param classification
     * @param bomCharacteristicMaster
     * @param bomCharacteristicMasterLastModified
     * @param order
     * @param title
     * @param description
     * @param type
     * @param numberMaxIntegerPlaces
     * @param numberMaxDecimalPlaces
     * @param numberDefaultValue
     * @param numberValue
     * @param stringMaxLength
     * @param stringDefaultValue
     * @param stringValue
     * @param dateDefaultValue
     * @param dateValue
     * @param booleanDefaultValue
     * @param booleanValue
     * @param listContent
     * @param listDefaultValue
     * @param listValue
     * @param unit
     * @param showInReport
     * @param orderInReport
     * @param required
     * @param includeInTitle
     * @param unmodifiable
     * @param messageType
     * @param message
     */
    public BomCharacteristic(String bomCharacteristicId, String classification,
            String bomCharacteristicMaster,
            Date bomCharacteristicMasterLastModified, Long order, String title,
            String description, String type, Long numberMaxIntegerPlaces,
            Long numberMaxDecimalPlaces, String numberDefaultValue,
            String numberValue, Long stringMaxLength, String stringDefaultValue,
            String stringValue, Date dateDefaultValue, Date dateValue,
            Boolean booleanDefaultValue, Boolean booleanValue,
            List<Map<String, String>> listContent, String listDefaultValue,
            String listValue, String unit, Boolean showInReport,
            Long orderInReport, Boolean required, Boolean includeInTitle,
            Boolean unmodifiable, String messageType, String message) {
        super();
        this.bomCharacteristicId = bomCharacteristicId;
        this.classification = classification;
        this.bomCharacteristicMaster = bomCharacteristicMaster;
        this.bomCharacteristicMasterLastModified = bomCharacteristicMasterLastModified;
        this.order = order;
        this.title = title;
        this.description = description;
        this.type = type;
        this.numberMaxIntegerPlaces = numberMaxIntegerPlaces;
        this.numberMaxDecimalPlaces = numberMaxDecimalPlaces;
        this.numberDefaultValue = numberDefaultValue;
        this.numberValue = numberValue;
        this.stringMaxLength = stringMaxLength;
        this.stringDefaultValue = stringDefaultValue;
        this.stringValue = stringValue;
        this.dateDefaultValue = dateDefaultValue;
        this.dateValue = dateValue;
        this.booleanDefaultValue = booleanDefaultValue;
        this.booleanValue = booleanValue;
        this.listContent = listContent;
        this.listDefaultValue = listDefaultValue;
        this.listValue = listValue;
        this.unit = unit;
        this.showInReport = showInReport;
        this.orderInReport = orderInReport;
        this.required = required;
        this.includeInTitle = includeInTitle;
        this.unmodifiable = unmodifiable;
        this.messageType = messageType;
        this.message = message;
    }

    public String getBomCharacteristicId() {
        return bomCharacteristicId;
    }

    public void setBomCharacteristicId(String bomCharacteristicId) {
        this.bomCharacteristicId = bomCharacteristicId;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getBomCharacteristicMaster() {
        return bomCharacteristicMaster;
    }

    public void setBomCharacteristicMaster(String bomCharacteristicMaster) {
        this.bomCharacteristicMaster = bomCharacteristicMaster;
    }

    public Date getBomCharacteristicMasterLastModified() {
        return bomCharacteristicMasterLastModified;
    }

    public void setBomCharacteristicMasterLastModified(
            Date bomCharacteristicMasterLastModified) {
        this.bomCharacteristicMasterLastModified = bomCharacteristicMasterLastModified;
    }

    public Long getOrder() {
        return order;
    }

    public void setOrder(Long order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getNumberMaxIntegerPlaces() {
        return numberMaxIntegerPlaces;
    }

    public void setNumberMaxIntegerPlaces(Long numberMaxIntegerPlaces) {
        this.numberMaxIntegerPlaces = numberMaxIntegerPlaces;
    }

    public Long getNumberMaxDecimalPlaces() {
        return numberMaxDecimalPlaces;
    }

    public void setNumberMaxDecimalPlaces(Long numberMaxDecimalPlaces) {
        this.numberMaxDecimalPlaces = numberMaxDecimalPlaces;
    }

    public String getNumberDefaultValue() {
        return numberDefaultValue;
    }

    public void setNumberDefaultValue(String numberDefaultValue) {
        this.numberDefaultValue = numberDefaultValue;
    }

    public String getNumberValue() {
        return numberValue;
    }

    public void setNumberValue(String numberValue) {
        this.numberValue = numberValue;
    }

    public Long getStringMaxLength() {
        return stringMaxLength;
    }

    public void setStringMaxLength(Long stringMaxLength) {
        this.stringMaxLength = stringMaxLength;
    }

    public String getStringDefaultValue() {
        return stringDefaultValue;
    }

    public void setStringDefaultValue(String stringDefaultValue) {
        this.stringDefaultValue = stringDefaultValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Date getDateDefaultValue() {
        return dateDefaultValue;
    }

    public void setDateDefaultValue(Date dateDefaultValue) {
        this.dateDefaultValue = dateDefaultValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Boolean getBooleanDefaultValue() {
        return booleanDefaultValue;
    }

    public void setBooleanDefaultValue(Boolean booleanDefaultValue) {
        this.booleanDefaultValue = booleanDefaultValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public List<Map<String, String>> getListContent() {
        return listContent;
    }

    public void setListContent(List<Map<String, String>> listContent) {
        this.listContent = listContent;
    }

    public String getListDefaultValue() {
        return listDefaultValue;
    }

    public void setListDefaultValue(String listDefaultValue) {
        this.listDefaultValue = listDefaultValue;
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

    public Boolean getShowInReport() {
        return showInReport;
    }

    public void setShowInReport(Boolean showInReport) {
        this.showInReport = showInReport;
    }

    public Long getOrderInReport() {
        return orderInReport;
    }

    public void setOrderInReport(Long orderInReport) {
        this.orderInReport = orderInReport;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getIncludeInTitle() {
        return includeInTitle;
    }

    public void setIncludeInTitle(Boolean includeInTitle) {
        this.includeInTitle = includeInTitle;
    }

    public Boolean getUnmodifiable() {
        return unmodifiable;
    }

    public void setUnmodifiable(Boolean unmodifiable) {
        this.unmodifiable = unmodifiable;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void appendMessage(String message) {
        if (this.message == null || this.message.isEmpty()) {
            setMessage(message);
        } else {
            this.message += " " + message;
        }

    }

}
