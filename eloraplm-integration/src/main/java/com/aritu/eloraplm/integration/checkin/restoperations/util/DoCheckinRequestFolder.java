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
public class DoCheckinRequestFolder {

    private int dbId;

    private int localId;

    private DocumentRef wcRef;

    private DocumentRef parentRealRef;

    public DoCheckinRequestFolder(int dbId, int localId, DocumentRef wcRef,
            DocumentRef parentRealRef) {
        this.dbId = dbId;
        this.localId = localId;
        this.wcRef = wcRef;
        this.parentRealRef = parentRealRef;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public DocumentRef getWcRef() {
        return wcRef;
    }

    public void setWcRef(DocumentRef wcRef) {
        this.wcRef = wcRef;
    }

    public DocumentRef getParentRealRef() {
        return parentRealRef;
    }

    public void setParentRealRef(DocumentRef parentRealRef) {
        this.parentRealRef = parentRealRef;
    }

}
