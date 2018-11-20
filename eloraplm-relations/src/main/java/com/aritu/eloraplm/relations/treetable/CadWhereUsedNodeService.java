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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class CadWhereUsedNodeService extends RelationNodeService
        implements NodeManager {

    private boolean showDrawings;

    private boolean showSuppressed;

    public CadWhereUsedNodeService(CoreSession session, boolean showDrawings,
            boolean showSuppressed, boolean showObsoleteStateDocuments,
            boolean showUniqueVersionsPerDocument) throws EloraException {

        super(session);

        nodeId = 0;
        treeDirection = TREE_DIRECTION_WHERE_USED;
        relationType = RELATION_TYPE_CAD;

        this.showDrawings = showDrawings;
        this.showSuppressed = showSuppressed;
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;

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
     * @param comment
     * @param isSpecial
     * @return
     */
    @Override
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, String quantity,
            String comment, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, boolean isSpecial, boolean isDirect) {

        CadRelationNodeData nodeData = new CadRelationNodeData(id, level, docId,
                data, wcDoc, stmt, predicateUri, quantity, comment, ordering,
                directorOrdering, viewerOrdering, isSpecial, isDirect);

        nodeData = loadRelatedItems(nodeData);

        boolean isBasedOn = (predicateUri != null
                && predicateUri.equals(EloraRelationConstants.CAD_BASED_ON))
                        ? true : false;
        nodeData.setIsBasedOn(isBasedOn);

        boolean isSuppressed = false;
        if (showSuppressed && !isSpecial && !isDirect
                && RelationsConfig.cadSuppressedRelationsList.contains(
                        predicateUri)) {
            isSuppressed = true;
        }
        nodeData.setIsSuppressed(isSuppressed);

        return nodeData;
    }

    protected CadRelationNodeData loadRelatedItems(
            CadRelationNodeData nodeData) {

        DocumentModelList relatedItemList = getRelatedItems(nodeData);
        if (relatedItemList != null && !relatedItemList.isEmpty()) {
            List<DocumentModel> relatedItems = new ArrayList<DocumentModel>();
            List<String> processedVersionSeries = new ArrayList<String>();
            for (DocumentModel relatedItem : relatedItemList) {
                String versionSeriesId = relatedItem.getVersionSeriesId();
                // We only add each related document once, as the query is
                // ordered by modified time (desc), we supose it will be the
                // latest
                if (!processedVersionSeries.contains(versionSeriesId)) {
                    relatedItems.add(relatedItem);
                    processedVersionSeries.add(versionSeriesId);
                }
            }

            nodeData.setRelatedBoms(relatedItems);
        }

        return nodeData;

    }

    protected DocumentModelList getRelatedItems(CadRelationNodeData nodeData) {
        DocumentModelList relatedItems = null;

        DocumentModel cadDocument = nodeData.getData();

        String predicateUri = EloraRelationConstants.BOM_HAS_CAD_DOCUMENT;
        Resource predicateResource = new ResourceImpl(predicateUri);

        relatedItems = RelationHelper.getSubjectDocuments(
                EloraRelationConstants.ELORA_GRAPH_NAME, predicateResource,
                cadDocument);

        return relatedItems;
    }

    private void loadConfigurations() throws EloraException {

        loadDirectRelations();
        loadPredicates();
    }

    private void loadDirectRelations() {
        directRelationsList = new ArrayList<>(
                RelationsConfig.cadDirectRelationsList);
    }

    private void loadPredicates() {
        List<String> predicatesList = new ArrayList<String>();

        // We don't treat the drawings as special relations
        if (showDrawings) {
            predicatesList.addAll(RelationsConfig.cadSpecialRelationsList);
        }

        predicatesList.addAll(RelationsConfig.cadHierarchicalRelationsList);
        if (showSuppressed) {
            predicatesList.addAll(RelationsConfig.cadSuppressedRelationsList);
        }

        predicatesList.addAll(directRelationsList);

        loadPredicateResources(predicatesList);
    }

    private void loadPredicateResources(List<String> predicatesList) {
        for (String predicateUri : predicatesList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            predicates.add(predicateResource);
        }
    }

}
