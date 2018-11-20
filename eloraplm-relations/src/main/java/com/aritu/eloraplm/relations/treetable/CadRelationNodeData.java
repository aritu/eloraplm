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
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

/**
 * @author aritu
 *
 */
public class CadRelationNodeData extends BaseRelationNodeData {

    private static final long serialVersionUID = 1L;

    private boolean isBasedOn;

    private boolean isSuppressed;

    private List<DocumentModel> relatedBoms;

    public CadRelationNodeData(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, String quantity, String comment,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            boolean isSpecial, boolean isDirect) {

        super(id, level, docId, data, wcDoc, stmt, predicateUri, quantity,
                comment, ordering, directorOrdering, viewerOrdering, isSpecial,
                isDirect);

        relatedBoms = new ArrayList<DocumentModel>();
    }

    public List<DocumentModel> getRelatedBoms() {
        return relatedBoms;
    }

    public void setRelatedBoms(List<DocumentModel> relatedBoms) {
        this.relatedBoms = relatedBoms;
    }

    public boolean getIsBasedOn() {
        return isBasedOn;
    }

    public void setIsBasedOn(boolean isBasedOn) {
        this.isBasedOn = isBasedOn;
    }

    public boolean getIsSuppressed() {
        return isSuppressed;
    }

    public void setIsSuppressed(boolean isSuppressed) {
        this.isSuppressed = isSuppressed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((relatedBoms == null) ? 0 : relatedBoms.hashCode());
        result = prime * result + (isBasedOn ? 1231 : 1237);
        result = prime * result + (isSuppressed ? 1231 : 1237);
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
        CadRelationNodeData other = (CadRelationNodeData) obj;
        if (relatedBoms == null) {
            if (other.relatedBoms != null) {
                return false;
            }
        } else if (!relatedBoms.equals(other.relatedBoms)) {
            return false;
        }
        if (isBasedOn != other.isBasedOn) {
            return false;
        }
        if (isSuppressed != other.isSuppressed) {
            return false;
        }
        return true;
    }

}