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
package com.aritu.eloraplm.bom.lists.util;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

public class BomListEntry {

    // TODO: THIS CLASS MUST BE IMPLEMENTED WHEN COMPLETING BOM LIST
    // FUNCTIONALITY

    protected String bomListId;

    protected String reference;

    protected String name;

    // suppliers code list
    protected List<String> suppliers;

    // Document in Elora
    protected DocumentModel document;

    /**
     * @param bomListId
     * @param reference
     * @param name
     * @param suppliers
     * @param document
     */
    public BomListEntry(String bomListId, String reference, String name,
            List<String> suppliers, DocumentModel document) {
        super();
        this.bomListId = bomListId;
        this.reference = reference;
        this.name = name;
        this.suppliers = suppliers;
        this.document = document;
    }

    public String getBomListId() {
        return bomListId;
    }

    public void setBomListId(String bomListId) {
        this.bomListId = bomListId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<String> suppliers) {
        this.suppliers = suppliers;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public void setDocument(DocumentModel document) {
        this.document = document;
    }

}