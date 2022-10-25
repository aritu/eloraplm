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

import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author aritu
 *
 */
public class TryCheckinRequestDoc {

    private int localId;

    private DocumentRef realRef;

    private DocumentRef wcRef;

    private String type;

    private String filename;

    private Properties properties;

    private DocumentRef structureRootRealRef;

    private boolean overwrite;

    public TryCheckinRequestDoc(int localId, DocumentRef realRef,
            DocumentRef wcRef, String type, String filename,
            DocumentRef structureRootRealRef, boolean overwrite) {
        this.localId = localId;
        this.realRef = realRef;
        this.wcRef = wcRef;
        this.type = type;
        this.filename = filename;
        this.structureRootRealRef = structureRootRealRef;
        this.overwrite = overwrite;
        // Initialize properties
        properties = new Properties();
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public DocumentRef getRealRef() {
        return realRef;
    }

    public void setRealRef(DocumentRef realRef) {
        this.realRef = realRef;
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Properties getProperties() {
        return properties;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public DocumentRef getStructureRootRealRef() {
        return structureRootRealRef;
    }

    public void setStructureRootRealRef(DocumentRef structureRootRealRef) {
        this.structureRootRealRef = structureRootRealRef;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
}
