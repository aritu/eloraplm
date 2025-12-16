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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This class encapsulates the attributes related to a Linked Characteristic
 *
 * @author aritu
 *
 */
public class LinkedCharacteristic {

    protected DocumentModel linkedDocument;

    protected LinkedCharacteristicInfo linkedCharacteristicInfo;

    protected Boolean showInReport;

    protected Long orderInReport;

    public LinkedCharacteristic(DocumentModel linkedDocument,
            LinkedCharacteristicInfo linkedCharacteristicInfo,
            Boolean showInReport, Long orderInReport) {
        super();
        this.linkedDocument = linkedDocument;
        this.linkedCharacteristicInfo = linkedCharacteristicInfo;
        this.showInReport = showInReport;
        this.orderInReport = orderInReport;
    }

    public DocumentModel getLinkedDocument() {
        return linkedDocument;
    }

    public void setLinkedDocument(DocumentModel linkedDocument) {
        this.linkedDocument = linkedDocument;
    }

    public LinkedCharacteristicInfo getLinkedCharacteristicInfo() {
        return linkedCharacteristicInfo;
    }

    public void setLinkedCharacteristicInfo(
            LinkedCharacteristicInfo linkedCharacteristicInfo) {
        this.linkedCharacteristicInfo = linkedCharacteristicInfo;
    }

    public Boolean getShowInReport() {
        return showInReport;
    }

    public void setShowInReport(Boolean showInReport) {
        this.showInReport = showInReport;
        if (!showInReport) {
            setOrderInReport(0L);
        }
    }

    public Long getOrderInReport() {
        return orderInReport;
    }

    public void setOrderInReport(Long orderInReport) {
        this.orderInReport = orderInReport;
    }

}
