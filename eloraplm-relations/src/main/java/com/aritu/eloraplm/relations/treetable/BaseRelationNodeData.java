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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * @author aritu
 *
 */
public class BaseRelationNodeData extends BaseNodeData
        implements RelationNodeData {
    private static final long serialVersionUID = 1L;

    private String docId;

    private DocumentModel data;

    private DocumentModel wcDoc;

    // TODO Statementa gehitu danez, predicateUri, quantity, etab. sobran dauz,
    // baina
    // lortu behar da moduren bat balio horreik gordetzeko benetan erlazio bat
    // sortu gabe. graph.add() ein barik posible da?
    private Statement stmt;

    private String predicateUri;

    private String quantity;

    private String comment;

    private Integer ordering;

    private Integer directorOrdering;

    private Integer viewerOrdering;

    private List<String> iconOnlyRelations;

    private boolean isSpecial;

    // It is used to check if we have to obtain child relations. If it is
    // (conceptual) direct
    private boolean isDirect;

    private Map<String, String> versionList;

    public BaseRelationNodeData(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, String quantity, String comment,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            boolean isSpecial, boolean isDirect) {
        this(id, level, docId, data, wcDoc, stmt, predicateUri, quantity,
                comment, ordering, directorOrdering, viewerOrdering, isSpecial,
                isDirect, false, false);
    }

    public BaseRelationNodeData(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, String quantity, String comment,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            boolean isSpecial, boolean isDirect, boolean isNew,
            boolean isRemoved) {
        super(id, level, isNew, isRemoved);

        this.docId = docId;
        this.data = data;
        this.wcDoc = wcDoc;
        this.stmt = stmt;
        this.predicateUri = predicateUri;
        this.quantity = quantity;
        this.comment = comment;
        this.ordering = ordering;
        this.directorOrdering = directorOrdering;
        this.viewerOrdering = viewerOrdering;
        this.isSpecial = isSpecial;
        this.isDirect = isDirect;

        versionList = new LinkedHashMap<String, String>();
        if (docId != null && data != null) {
            String versionLabel = data.getVersionLabel();
            if (!data.isImmutable()) {
                versionLabel += " (WC)";
            }
            versionList.put(docId, versionLabel);
        }

        iconOnlyRelations = new ArrayList<String>();
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
    public String getQuantity() {
        return quantity;
    }

    @Override
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public Integer getOrdering() {
        return ordering;
    }

    @Override
    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    @Override
    public Integer getDirectorOrdering() {
        return directorOrdering;
    }

    @Override
    public void setDirectorOrdering(Integer directorOrdering) {
        this.directorOrdering = directorOrdering;
    }

    @Override
    public Integer getViewerOrdering() {
        return viewerOrdering;
    }

    @Override
    public void setViewerOrdering(Integer viewerOrdering) {
        this.viewerOrdering = viewerOrdering;
    }

    @Override
    public boolean getIsSpecial() {
        return isSpecial;
    }

    @Override
    public void setIsSpecial(boolean isSpecial) {
        this.isSpecial = isSpecial;
    }

    @Override
    public boolean getIsDirect() {
        return isDirect;
    }

    public void setIsDirect(boolean isDirect) {
        this.isDirect = isDirect;
    }

    @Override
    public List<String> getIconOnlyRelations() {
        return iconOnlyRelations;
    }

    @Override
    public void setIconOnlyRelations(List<String> iconOnlyRelations) {
        this.iconOnlyRelations = iconOnlyRelations;
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
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        result = prime * result + ((docId == null) ? 0 : docId.hashCode());
        result = prime * result + ((iconOnlyRelations == null) ? 0
                : iconOnlyRelations.hashCode());
        result = prime * result + (isSpecial ? 1231 : 1237);
        result = prime * result + ordering;
        result = prime * result + directorOrdering;
        result = prime * result + viewerOrdering;
        result = prime * result
                + ((predicateUri == null) ? 0 : predicateUri.hashCode());
        result = prime * result
                + ((quantity == null) ? 0 : quantity.hashCode());
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
        BaseRelationNodeData other = (BaseRelationNodeData) obj;
        if (comment == null) {
            if (other.comment != null) {
                return false;
            }
        } else if (!comment.equals(other.comment)) {
            return false;
        }
        if (docId == null) {
            if (other.docId != null) {
                return false;
            }
        } else if (!docId.equals(other.docId)) {
            return false;
        }
        if (iconOnlyRelations == null) {
            if (other.iconOnlyRelations != null) {
                return false;
            }
        } else if (!iconOnlyRelations.equals(other.iconOnlyRelations)) {
            return false;
        }
        if (isSpecial != other.isSpecial) {
            return false;
        }
        if (ordering != other.ordering) {
            return false;
        }
        if (directorOrdering != other.directorOrdering) {
            return false;
        }
        if (viewerOrdering != other.viewerOrdering) {
            return false;
        }
        if (predicateUri == null) {
            if (other.predicateUri != null) {
                return false;
            }
        } else if (!predicateUri.equals(other.predicateUri)) {
            return false;
        }
        if (quantity != other.quantity) {
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
        BaseRelationNodeData objNode = (BaseRelationNodeData) obj;
        return getDocId().compareTo(objNode.getDocId());
    }
}