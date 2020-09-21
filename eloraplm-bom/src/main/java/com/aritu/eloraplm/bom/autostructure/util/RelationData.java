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
package com.aritu.eloraplm.bom.autostructure.util;

import java.math.BigDecimal;

import org.nuxeo.ecm.core.api.DocumentModel;

public class RelationData {
    private BigDecimal quantity;

    private Integer ordering;

    private Integer directorOrdering;

    private Boolean isManual;

    private DocumentModel objItem;

    public RelationData(Integer ordering, Integer directorOrdering,
            BigDecimal quantity, Boolean isManual, DocumentModel objItem) {
        this.ordering = ordering;
        this.directorOrdering = directorOrdering;
        this.quantity = quantity;
        this.isManual = isManual;
        this.objItem = objItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Boolean getIsManual() {
        return isManual;
    }

    public void addQuantity(BigDecimal qty) {
        quantity = quantity.add(qty);
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    public Integer getDirectorOrdering() {
        return directorOrdering;
    }

    public DocumentModel getObjItem() {
        return objItem;
    }

    public void setObjItem(DocumentModel item) {
        objItem = item;
    }
}