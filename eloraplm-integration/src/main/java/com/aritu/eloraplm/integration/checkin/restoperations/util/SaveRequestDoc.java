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

import java.util.List;

import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.DocumentRef;

import com.aritu.eloraplm.core.util.EloraFileInfo;

/**
 * @author aritu
 *
 */
public class SaveRequestDoc {

    private int localId;

    private DocumentRef realRef;

    private DocumentRef wcRef;

    private DocumentRef parentRealRef;

    private String type;

    private String filename;

    private Properties properties;

    private EloraFileInfo contentFile;

    private EloraFileInfo eloraViewerFile;

    private List<EloraFileInfo> cadAttachments;

    private DocumentRef structureRootRealRef;

    public SaveRequestDoc(int localId, DocumentRef realRef, DocumentRef wcRef,
            DocumentRef parentRealRef, String type, String filename,
            DocumentRef structureRootRealRef) {
        this.localId = localId;
        this.realRef = realRef;
        this.wcRef = wcRef;
        this.parentRealRef = parentRealRef;
        this.type = type;
        this.filename = filename;
        this.structureRootRealRef = structureRootRealRef;
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

    public DocumentRef getParentRealRef() {
        return parentRealRef;
    }

    public void setParentRealRef(DocumentRef parentRealRef) {
        this.parentRealRef = parentRealRef;
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

    public EloraFileInfo getContentFile() {
        return contentFile;
    }

    public void setContentFile(int fileId, String batch, String hash) {
        contentFile = new EloraFileInfo(fileId, "", batch, hash);
    }

    public EloraFileInfo getViewerFile() {
        return eloraViewerFile;
    }

    public void setViewerFile(int fileId, String fileName, String batch,
            String hash) {
        eloraViewerFile = new EloraFileInfo(fileId, fileName, batch, hash);
    }

    public List<EloraFileInfo> getCadAttachments() {
        return cadAttachments;
    }

    public void addCadAttachmentFile(int fileId, String fileName, String batch,
            String hash) {
        cadAttachments.add(new EloraFileInfo(fileId, fileName, batch, hash));
    }

    public DocumentRef getStructureRootRealRef() {
        return structureRootRealRef;
    }

    public void setStructureRootRealRef(DocumentRef structureRootRealRef) {
        this.structureRootRealRef = structureRootRealRef;
    }
}
