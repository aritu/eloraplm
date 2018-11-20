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
public class TryCheckinRequestFolder {

    private int localId;

    private DocumentRef wcRef;

    private String type;

    private String title;

    private DocumentRef structureRootRealRef;

    public TryCheckinRequestFolder(int localId, DocumentRef wcRef, String type,
            String title, DocumentRef structureRootRealRef) {
        this.localId = localId;
        this.wcRef = wcRef;
        this.type = type;
        this.title = title;
        this.structureRootRealRef = structureRootRealRef;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentRef getStructureRootRealRef() {
        return structureRootRealRef;
    }

    public void setStructureRootRealRef(DocumentRef structureRootRealRef) {
        this.structureRootRealRef = structureRootRealRef;
    }

}
