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
package com.aritu.eloraplm.relations.treetable;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.treetable.CoreNodeDataImpl;

/**
 * @author aritu
 *
 */
public class CoreRelationNodeDataImpl extends CoreNodeDataImpl
        implements CoreRelationNodeData {
    private static final long serialVersionUID = 1L;

    private String docId;

    private DocumentModel data;

    private DocumentModel wcDoc;

    // TODO Statementa gehitu danez, predicateUri, quantity, etab. sobran dauz,
    // baina lortu behar da moduren bat balio horreik gordetzeko benetan erlazio
    // bat sortu gabe. graph.add() ein barik posible da?
    private Statement stmt;

    private String predicateUri;

    private Map<String, String> versionList;

    public CoreRelationNodeDataImpl(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri) {
        this(id, level, docId, data, wcDoc, stmt, predicateUri, false, false);
    }

    public CoreRelationNodeDataImpl(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, boolean isNew, boolean isRemoved) {

        super(id, level, isNew, isRemoved);

        this.docId = docId;
        this.data = data;
        this.wcDoc = wcDoc;
        this.stmt = stmt;
        this.predicateUri = predicateUri;

        versionList = new LinkedHashMap<String, String>();
        if (docId != null && data != null) {
            String versionLabel = EloraDocumentHelper.getVersionWithExternalIfExists(data);
            versionList.put(docId, versionLabel);
        }
    }

    @Override
    public String getDocId() {
        return docId;
    }

    @Override
    public void setDocId(String docId) {
        this.docId = docId;
    }

    @Override
    public DocumentModel getData() {
        return data;
    }

    @Override
    public void setData(DocumentModel data) {
        this.data = data;
    }

    @Override
    public DocumentModel getWcDoc() {
        return wcDoc;
    }

    @Override
    public void setWcDoc(DocumentModel wcDoc) {
        this.wcDoc = wcDoc;
    }

    @Override
    public Statement getStmt() {
        return stmt;
    }

    @Override
    public void setStmt(Statement stmt) {
        this.stmt = stmt;
    }

    @Override
    public String getPredicateUri() {
        return predicateUri;
    }

    @Override
    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    @Override
    public Map<String, String> getVersionList() {
        return versionList;
    }

    @Override
    public void setVersionList(Map<String, String> versionList) {
        this.versionList = versionList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((docId == null) ? 0 : docId.hashCode());
        result = prime * result
                + ((predicateUri == null) ? 0 : predicateUri.hashCode());
        result = prime * result
                + ((versionList == null) ? 0 : versionList.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CoreRelationNodeDataImpl other = (CoreRelationNodeDataImpl) obj;
        if (docId == null) {
            if (other.docId != null) {
                return false;
            }
        } else if (!docId.equals(other.docId)) {
            return false;
        }
        if (predicateUri == null) {
            if (other.predicateUri != null) {
                return false;
            }
        } else if (!predicateUri.equals(other.predicateUri)) {
            return false;
        }
        if (versionList == null) {
            if (other.versionList != null) {
                return false;
            }
        } else if (!versionList.equals(other.versionList)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object obj) {
        CoreRelationNodeDataImpl objNode = (CoreRelationNodeDataImpl) obj;
        return getDocId().compareTo(objNode.getDocId());
    }
}