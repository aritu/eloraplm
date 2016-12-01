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

    protected List<DocumentModel> relatedBoms;

    public CadRelationNodeData(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, int quantity, String comment,
            boolean isObjectWc, int ordering, boolean isSpecial) {

        super(id, level, docId, data, wcDoc, stmt, predicateUri, quantity,
                comment, isObjectWc, ordering, isSpecial);

        relatedBoms = new ArrayList<DocumentModel>();
    }

    public List<DocumentModel> getRelatedBoms() {
        return relatedBoms;
    }

    public void setRelatedBoms(List<DocumentModel> relatedBoms) {
        this.relatedBoms = relatedBoms;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((relatedBoms == null) ? 0 : relatedBoms.hashCode());
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
        return true;
    }

}