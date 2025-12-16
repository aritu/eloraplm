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
package com.aritu.eloraplm.doctypes.datatable;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.datatable.BaseRowData;

/**
 *
 * @author aritu
 *
 */
public class DocumentVersionRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private DocumentModel versionDoc;

    private String type;

    private String reference;

    private String version;

    private String sourceVersion;

    private boolean nonCorrelativeVersion;

    private String comment;

    private String lifecycleState;

    private String lastContributor;

    public DocumentVersionRowData(String id, DocumentModel versionDoc,
            String type, String reference, String version, String sourceVersion,
            boolean nonCorrelativeVersion, String comment,
            String lifecycleState, String lastContributor, boolean isNew,
            boolean isModified, boolean isRemoved) {
        super(id, isNew, isModified, isRemoved);

        this.versionDoc = versionDoc;
        this.type = type;
        this.reference = reference;
        this.version = version;
        this.sourceVersion = sourceVersion;
        this.nonCorrelativeVersion = nonCorrelativeVersion;
        this.comment = comment;
        this.lifecycleState = lifecycleState;
        this.lastContributor = lastContributor;
    }

    public DocumentModel getVersionDoc() {
        return versionDoc;
    }

    public void setVersionDoc(DocumentModel versionDoc) {
        this.versionDoc = versionDoc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public boolean getNonCorrelativeVersion() {
        return nonCorrelativeVersion;
    }

    public void setNonCorrelativeVersion(boolean nonCorrelativeVersion) {
        this.nonCorrelativeVersion = nonCorrelativeVersion;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

}
