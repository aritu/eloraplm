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
package com.aritu.eloraplm.bom.treetable;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeService;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class BomCompositionNodeService extends RelationNodeService
        implements NodeManager {

    protected boolean showDirectDocuments;

    public BomCompositionNodeService(CoreSession session,
            boolean showUniqueVersionsPerDocument,
            boolean showObsoleteStateDocuments, boolean showDirectDocuments)
            throws EloraException {
        super(session);

        treeDirection = TREE_DIRECTION_COMPOSITION;

        relationType = RELATION_TYPE_BOM;

        nodeId = 0;

        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
        this.showDirectDocuments = showDirectDocuments;

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
            boolean isSpecial, boolean isDirect) {

        return new BaseRelationNodeData(id, level, docId, data, wcDoc, stmt,
                predicateUri, quantity, comment, ordering, directorOrdering,
                viewerOrdering, inverseViewerOrdering, isSpecial, isDirect);
    }

    private void loadConfigurations() throws EloraException {
        loadDirectRelations();
        loadPredicates();
    }

    private void loadDirectRelations() {
        directRelationsList = new ArrayList<>(
                RelationsConfig.bomDirectRelationsList);
    }

    private void loadPredicates() {
        List<String> predicatesList = new ArrayList<String>();
        predicatesList.addAll(RelationsConfig.bomHierarchicalRelationsList);

        if (showDirectDocuments) {
            predicatesList.addAll(directRelationsList);
        }

        loadPredicateResources(predicatesList);
    }

    private void loadPredicateResources(List<String> predicatesList) {
        for (String predicateUri : predicatesList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            predicates.add(predicateResource);
        }
    }

}
