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
package com.aritu.eloraplm.integration.get.restoperations.util;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class UidRequestDoc {

    private DocumentRef wcRef;

    private DocumentRef realRef;

    private DocumentRef parentRealRef;

    public UidRequestDoc(DocumentRef wcRef, DocumentRef realRef,
            DocumentRef parentRealRef) {
        this.wcRef = wcRef;
        this.realRef = realRef;
        this.parentRealRef = parentRealRef;
    }

    public DocumentRef getWcRef() {
        return wcRef;
    }

    public void setWcRef(DocumentRef wcRef) {
        this.wcRef = wcRef;
    }

    public DocumentRef getRealRef() {
        return realRef;
    }

    public void setRealRef(DocumentRef realRef) {
        this.realRef = realRef;
    }

    public DocumentRef getParentRealRef() {
        return parentRealRef;
    }

    public void setParentRealRef(DocumentRef parentRealRef) {
        this.parentRealRef = parentRealRef;
    }

}
