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

import com.aritu.eloraplm.integration.restoperations.util.CadFileInfoRequestDoc;

/**
 * @author aritu
 *
 */
public class DoCheckinRequestDoc extends CadFileInfoRequestDoc {

    private int dbId;

    private int localId;

    private DocumentRef wcRef;

    private DocumentRef parentRealRef;

    private String comment;

    private boolean unlock;

    private DocumentRef structureRootRealRef;

    private boolean overwrite;

    private ForceMetadataInfo forceMetadata;

    public DoCheckinRequestDoc(int dbId, int localId, DocumentRef wcRef,
            DocumentRef parentRealRef, String comment, boolean unlock,
            DocumentRef structureRootRealRef, boolean overwrite) {
        super();
        this.dbId = dbId;
        this.localId = localId;
        this.parentRealRef = parentRealRef;
        this.wcRef = wcRef;
        this.comment = comment;
        this.unlock = unlock;
        this.structureRootRealRef = structureRootRealRef;
        this.overwrite = overwrite;
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

    public ForceMetadataInfo getForceMetadata() {
        return forceMetadata;
    }

    public void setForceMetadata(ForceMetadataInfo forceMetadata) {
        this.forceMetadata = forceMetadata;
    }

}
