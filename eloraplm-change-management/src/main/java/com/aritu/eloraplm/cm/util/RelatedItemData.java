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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This class encapsulates the attributes of a Related Item.
 *
 * @author aritu
 *
 */
public class RelatedItemData {

    private DocumentModel docModel;

    private String quantity;

    /**
     * @param docModel
     * @param quantity
     */
    public RelatedItemData(DocumentModel docModel, String quantity) {
        super();
        this.docModel = docModel;
        this.quantity = quantity;
    }

    public DocumentModel getDocModel() {
        return docModel;
    }

    public void setDocModel(DocumentModel docModel) {
        this.docModel = docModel;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

}
