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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CadCompositionNodeService extends RelationNodeService {

    protected int maxOrdering;

    private boolean showDrawings;

    private boolean showSuppressed;

    private boolean showExcludedFromComposition;

    public int getMaxOrdering() {
        return maxOrdering;
    }

    public CadCompositionNodeService(CoreSession session, boolean showDrawings,
            boolean showSuppressed, boolean showExcludedFromComposition,
            boolean showObsoleteStateDocuments,
            boolean showUniqueVersionsPerDocument) throws EloraException {

        super(session);

        nodeId = 0;
        treeDirection = EloraRelationConstants.DIRECTION_COMPOSITION;
        relationType = RELATION_TYPE_CAD;
        maxOrdering = 0;

        this.showDrawings = showDrawings;
        this.showSuppressed = showSuppressed;
        this.showExcludedFromComposition = showExcludedFromComposition;
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
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual, String designators, Boolean isExcludedFromMapping,
            boolean isSpecial, boolean isDirect) {

        if (level == 1 && ordering != null && ordering > maxOrdering) {
            maxOrdering = ordering;
        }

        CadRelationNodeData nodeData = new CadRelationNodeData(id, level, docId,
                data, wcDoc, stmt, predicateUri, quantity, comment, ordering,
                directorOrdering, viewerOrdering, inverseViewerOrdering,
                isManual, designators, isExcludedFromMapping, isSpecial,
                isDirect);

        nodeData = loadRelatedItems(nodeData);

        boolean isBasedOn = (predicateUri != null
                && predicateUri.equals(EloraRelationConstants.CAD_BASED_ON))
                        ? true
                        : false;
        nodeData.setIsBasedOn(isBasedOn);

        boolean isSuppressed = false;
        if (predicateUri != null && showSuppressed && !isSpecial && !isDirect
                && RelationsConfig.cadSuppressedRelationsList.contains(
                        predicateUri)) {
            isSuppressed = true;
        }
        nodeData.setIsSuppressed(isSuppressed);

        boolean isExcludedFromComposition = false;
        if (predicateUri != null && showExcludedFromComposition && !isSpecial
                && !isDirect && predicateUri.equals(
                        EloraRelationConstants.CAD_HAS_EXCLUDED_FROM_COMPOSITION)) {
            isExcludedFromComposition = true;
        }
        nodeData.setIsExcludedFromComposition(isExcludedFromComposition);

        return nodeData;
    }

    protected CadRelationNodeData loadRelatedItems(
            CadRelationNodeData nodeData) {
        DocumentModelList relatedItemList = getRelatedItems(nodeData);
        if (relatedItemList != null && !relatedItemList.isEmpty()) {
            if (relatedItemList.size() > 1) {
                // It is possible to have different versions of the same bom //
                // item pointing to current document version. Take latest. //
                // related
                Map<String, List<DocumentModel>> docListByVersionSerieId = new HashMap<String, List<DocumentModel>>();
                for (DocumentModel relatedItem : relatedItemList) {
                    String versionSeriesId = session.getVersionSeriesId(
                            relatedItem.getRef());
                    if (docListByVersionSerieId.containsKey(versionSeriesId)) {
                        docListByVersionSerieId.get(versionSeriesId).add(
                                relatedItem);
                    } else {
                        List<DocumentModel> docList = new ArrayList<>();
                        docList.add(relatedItem);
                        docListByVersionSerieId.put(versionSeriesId, docList);
                    }
                }

                relatedItemList.removeAll(relatedItemList);

                if (docListByVersionSerieId.size() >= 1) {
                    for (Map.Entry<String, List<DocumentModel>> entry : docListByVersionSerieId.entrySet()) {
                        // If there are different versions of the same bom item
                        // related, take the latest version.
                        DocumentModel doc = null;
                        String versionSeriesId = entry.getKey();
                        List<DocumentModel> docList = docListByVersionSerieId.get(
                                versionSeriesId);
                        // TODO IBAI getLatestRelatedVersion(CoreSession
                        // session, List<DocumentModel> wcDocs) erabili eta
                        // testeatu
                        List<String> uidList = EloraDocumentHelper.getUidListFromDocList(
                                docList);
                        Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                                docList);
                        String type = docList.get(0).getType();

                        doc = EloraRelationHelper.getLatestRelatedVersion(
                                session, majorVersion, uidList, type);
                        relatedItemList.add(doc);
                    }

                }
            }
            nodeData.setRelatedBoms(relatedItemList);
        }
        /*
         * List<DocumentModel> relatedItems = new ArrayList<DocumentModel>();
         * List<String> processedVersionSeries = new ArrayList<String>(); for
         * (DocumentModel relatedItem : relatedItemList) { String
         * versionSeriesId = relatedItem.getVersionSeriesId(); // We only add
         * each related document once, as the query is // // ordered by modified
         * time (desc), we supose it will be the // // latest if
         * (!processedVersionSeries.contains(versionSeriesId)) {
         * relatedItems.add(relatedItem);
         * processedVersionSeries.add(versionSeriesId); } }
         *
         * nodeData.setRelatedBoms(relatedItems);
         */

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
        loadCadSpecialPredicates();
        loadPredicates();
    }

    private void loadDirectRelations() {
        directRelationsList = new ArrayList<>(
                RelationsConfig.cadDirectRelationsList);
    }

    private void loadCadSpecialPredicates() {
        List<String> cadSpecialPredicatesList = new ArrayList<String>();
        if (showDrawings) {
            cadSpecialPredicatesList.addAll(
                    RelationsConfig.cadSpecialRelationsList);

            loadCadSpecialPredicateResources(cadSpecialPredicatesList);
        }

    }

    private void loadPredicates() {
        List<String> predicatesList = new ArrayList<String>();

        List<String> hierarchicalPredicates = new ArrayList<String>(
                RelationsConfig.cadHierarchicalRelationsList);
        // Show excluded from composition?
        if (!showExcludedFromComposition) {
            hierarchicalPredicates.remove(
                    EloraRelationConstants.CAD_HAS_EXCLUDED_FROM_COMPOSITION);
        }
        predicatesList.addAll(hierarchicalPredicates);
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

    private void loadCadSpecialPredicateResources(List<String> predicatesList) {
        for (String predicateUri : predicatesList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            cadSpecialPredicates.add(predicateResource);
        }
    }
}
