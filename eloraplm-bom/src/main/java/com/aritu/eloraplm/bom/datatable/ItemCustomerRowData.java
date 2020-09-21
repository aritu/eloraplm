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
package com.aritu.eloraplm.bom.datatable;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.datatable.BaseRowData;

/**
 *
 * @author aritu
 *
 */
public class ItemCustomerRowData extends BaseRowData {

    private DocumentModel customerProductDoc;

    public ItemCustomerRowData(String id, DocumentModel customerProductDoc,
            boolean isNew, boolean isModified, boolean isRemoved) {
        super(id, isNew, isModified, isRemoved);
        this.customerProductDoc = customerProductDoc;
    }

    public DocumentModel getCustomerProductDoc() {
        return customerProductDoc;
    }

    public void setCustomerProductDoc(DocumentModel customerProductDoc) {
        this.customerProductDoc = customerProductDoc;
    }

}
