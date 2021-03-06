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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeService;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class CadDocumentsNodeService extends RelationNodeService
        implements NodeManager {

    public CadDocumentsNodeService(CoreSession session,
            boolean showObsoleteStateDocuments) throws EloraException {
        super(session);

        treeDirection = TREE_DIRECTION_COMPOSITION;

        relationType = RELATION_TYPE_DOC;

        nodeId = 0;

        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
        showUniqueVersionsPerDocument = false;

        loadConfigurations();
    }

    /**
     * Save the relationNodeData
     *
     * @param id
     * @param level
     * @param docId
     * @param data
     * @param predicateUri
     * @param quantity
     * @param isSpecial
     * @return
     */
    @Override
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, String quantity,
            String comment, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual, boolean isSpecial, boolean isDirect) {

        return new BaseRelationNodeData(id, level, docId, data, wcDoc, stmt,
                predicateUri, quantity, comment, ordering, directorOrdering,
                viewerOrdering, inverseViewerOrdering, isManual, isSpecial,
                isDirect);
    }

    private void loadConfigurations() throws EloraException {
        loadPredicates();
    }

    private void loadPredicates() {

        directRelationsList.add(EloraRelationConstants.CAD_HAS_DOCUMENT);
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.CAD_HAS_DOCUMENT);
        predicates.add(predicateResource);
    }

}
