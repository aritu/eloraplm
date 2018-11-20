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
package com.aritu.eloraplm.integration.checkin.restoperations.util;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author aritu
 *
 */
public class DoCheckinRequestRel {

    private DocumentRef objectRealRef;

    private DocumentRef subjectWcRef;

    private DocumentRef objectWcRef;

    private String predicate;

    private String quantity;

    private Integer ordering;

    public DoCheckinRequestRel(DocumentRef objectRealRef,
            DocumentRef subjectWcRef, DocumentRef objectWcRef, String predicate,
            String quantity, Integer ordering) {
        this.objectRealRef = objectRealRef;
        this.subjectWcRef = subjectWcRef;
        this.objectWcRef = objectWcRef;
        this.predicate = predicate;
        this.quantity = quantity;
        this.ordering = ordering;
    }

    public DocumentRef getObjectRealRef() {
        return objectRealRef;
    }

    public void setObjectRealRef(DocumentRef objectRealRef) {
        this.objectRealRef = objectRealRef;
    }

    public DocumentRef getSubjectWcRef() {
        return subjectWcRef;
    }

    public void setSubjectWcRef(DocumentRef subjectWcRef) {
        this.subjectWcRef = subjectWcRef;
    }

    public DocumentRef getObjectWcRef() {
        return objectWcRef;
    }

    public void setObjectWcRef(DocumentRef objectWcRef) {
        this.objectWcRef = objectWcRef;
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

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

}
