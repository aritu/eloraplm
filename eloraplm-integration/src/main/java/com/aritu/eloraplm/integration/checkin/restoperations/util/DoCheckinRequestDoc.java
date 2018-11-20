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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentRef;

import com.aritu.eloraplm.core.util.EloraFileInfo;

/**
 * @author aritu
 *
 */
public class DoCheckinRequestDoc {

    private int dbId;

    private int localId;

    private DocumentRef wcRef;

    private DocumentRef parentRealRef;

    private String comment;

    private boolean unlock;

    private EloraFileInfo contentFile;

    private EloraFileInfo eloraViewerFile;

    private List<EloraFileInfo> cadAttachments;

    private DocumentRef structureRootRealRef;

    public DoCheckinRequestDoc(int dbId, int localId, DocumentRef wcRef,
            DocumentRef parentRealRef, String comment, boolean unlock,
            DocumentRef structureRootRealRef) {
        this.dbId = dbId;
        this.localId = localId;
        this.parentRealRef = parentRealRef;
        this.wcRef = wcRef;
        this.comment = comment;
        this.unlock = unlock;
        this.structureRootRealRef = structureRootRealRef;
        cadAttachments = new ArrayList<EloraFileInfo>();
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isUnlock() {
        return unlock;
    }

    public void setUnlock(boolean unlock) {
        this.unlock = unlock;
    }

    public EloraFileInfo getContentFile() {
        return contentFile;
    }

    public void setContentFile(int fileId, String fileName, String batch,
            String hash) {
        contentFile = new EloraFileInfo(fileId, fileName, batch, hash);
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
