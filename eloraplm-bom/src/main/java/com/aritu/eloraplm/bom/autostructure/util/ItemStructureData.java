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
package com.aritu.eloraplm.bom.autostructure.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.CompositionWithMultipleVersionsException;
import com.aritu.eloraplm.exceptions.EloraException;

public class ItemStructureData {

    private static final Log log = LogFactory.getLog(ItemStructureData.class);

    private DocumentModel rootItem;

    private Set<String> directorItemList;

    private Set<String> currentItemList;

    private Map<String, RelationData> directorItemDataMap;

    private Map<String, RelationData> currentItemDataMap;

    private boolean hasDirector;

    private boolean hasStructure;

    private boolean structureUpdated;

    private Integer directorOrdering;

    private Integer maxItemGlobalOrdering;

    private Integer itemGlobalOrdering;

    private List<DocumentModel> missingItemList;

    private List<DocumentModel> wrongTypeItemList;

    // private Map<DocumentModel, List<DocumentModel>> multipleItemMap;

    private List<DocItemRelation> docItemRelationList;

    private List<DocItemRelation> selectedDocItemRelations;

    public DocumentModel getRootItem() {
        return rootItem;
    }

    public Set<String> getDirectorItemList() {
        return directorItemList;
    }

    public Set<String> getCurrentItemList() {
        return currentItemList;
    }

    public Map<String, RelationData> getDirectorItemDataMap() {
        return directorItemDataMap;
    }

    public Map<String, RelationData> getCurrentItemDataMap() {
        return currentItemDataMap;
    }

    public boolean hasStructure() {
        return hasStructure;
    }

    public boolean hasDirector() {
        return hasDirector;
    }

    public boolean isStructureUpdated() {
        return structureUpdated;
    }

    public void setIsStructureUpdated(boolean isUpdated) {
        structureUpdated = isUpdated;
    }

    public boolean hasMissingItems() {
        return missingItemList.size() > 0;
    }

    public List<DocumentModel> getMissingItemList() {
        return missingItemList;
    }

    // public Map<DocumentModel, List<DocumentModel>> getMultipleItemMap() {
    // return multipleItemMap;
    // }

    public List<DocItemRelation> getDocItemRelationList() {
        return docItemRelationList;
    }

    public List<DocumentModel> getWrongTypeItemList() {
        return wrongTypeItemList;
    }

    public boolean hasWrongTypes() {
        return wrongTypeItemList.size() > 0;
    }

    public void addWrongTypeItem(DocumentModel item) {
        wrongTypeItemList.add(item);
    }

    public ItemStructureData(DocumentModel item,
            List<DocItemRelation> selectedDocItemRelations, CoreSession session)
            throws CompositionWithMultipleVersionsException {
        this.selectedDocItemRelations = selectedDocItemRelations;
        rootItem = item;
        missingItemList = new ArrayList<DocumentModel>();
        wrongTypeItemList = new ArrayList<DocumentModel>();
        // multipleItemMap = new HashMap<DocumentModel, List<DocumentModel>>();
        docItemRelationList = new ArrayList<DocItemRelation>();
        hasDirector = false;
        hasStructure = false;
        structureUpdated = false;
        maxItemGlobalOrdering = 0;
        itemGlobalOrdering = 0;
        directorItemDataMap = new HashMap<String, RelationData>();
        directorItemList = new HashSet<String>();
        SortedMap<Integer, DocumentModelList> orderedDirectorDocMap = getNotDrawingDirectorDocuments(
                item, session);
        if (!orderedDirectorDocMap.isEmpty()) {
            currentItemList = getItemStructure(item, session);
            getDirectorsStructure(orderedDirectorDocMap, session);
        }
    }

    private SortedMap<Integer, DocumentModelList> getNotDrawingDirectorDocuments(
            DocumentModel item, CoreSession session) {
        String logInitMsg = "[getNotDrawingDirectorDocuments] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModelList docList;
        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, item,
                predicateResource);

        SortedMap<Integer, DocumentModelList> directorMap = new TreeMap<Integer, DocumentModelList>();
        for (Statement stmt : stmts) {
            EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(stmt);
            Integer directorOrd = stmtInfo.getDirectorOrdering();

            if (directorOrd != null) {
                DocumentModel object = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);

                if (EloraDocumentHelper.isWorkingCopy(object)
                        && !object.isCheckedOut()) {
                    object = EloraDocumentHelper.getBaseVersion(object);
                }

                if (!object.getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING)) {

                    if (!directorMap.containsKey(directorOrd)) {
                        docList = new DocumentModelListImpl();
                    } else {
                        docList = directorMap.get(directorOrd);
                    }
                    docList.add(object);
                    directorMap.put(directorOrd, docList);

                    log.trace(logInitMsg + "Document |" + object.getId()
                            + "| added as director");
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
        return directorMap;
    }

    private void getDirectorsStructure(
            SortedMap<Integer, DocumentModelList> orderedDirectorDocMap,
            CoreSession session)
            throws CompositionWithMultipleVersionsException {
        hasDirector = true;

        for (Map.Entry<Integer, DocumentModelList> entry : orderedDirectorDocMap.entrySet()) {
            DocumentModelList directorDocList = entry.getValue();
            directorOrdering = entry.getKey();
            for (DocumentModel directorDoc : directorDocList) {
                directorItemList.addAll(
                        getDirectorsItems(directorDoc, session));
            }
        }
        if (!directorItemList.isEmpty()) {
            hasStructure = true;
        }
    }

    private Set<String> getDirectorsItems(DocumentModel director,
            CoreSession session)
            throws CompositionWithMultipleVersionsException {
        List<Resource> predicates = getHierarchicalPredicates();
        List<Statement> directorStmts = EloraRelationHelper.getStatements(
                director, predicates);
        Set<String> structureItemList = new HashSet<String>();
        for (Statement directorStmt : directorStmts) {
            DocumentModel childDoc = RelationHelper.getDocumentModel(
                    directorStmt.getObject(), session);

            // TODO: Tener en cuenta si al hacer checkout del item queda
            // apuntado a
            // versiones anteriores. Mirar como afecta esto al cambio de
            // estructura
            Set<String> docItemList = getDocumentItems(childDoc, directorStmt,
                    session);
            structureItemList.addAll(docItemList);
        }
        maxItemGlobalOrdering = itemGlobalOrdering;

        return structureItemList;
    }

    private List<Resource> getHierarchicalPredicates() {
        List<Resource> predicates = new ArrayList<Resource>();
        for (String predicateUri : RelationsConfig.cadHierarchicalRelationsList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            predicates.add(predicateResource);
        }
        return predicates;
    }

    private Set<String> getDocumentItems(DocumentModel doc,
            Statement directorStmt, CoreSession session)
            throws CompositionWithMultipleVersionsException {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        List<Statement> itemStmts = EloraRelationHelper.getSubjectStatements(
                doc, predicateResource);

        Set<String> docItemList = new HashSet<String>();
        if (!itemStmts.isEmpty()) {
            Map<String, List<DocumentModel>> relatedItems = new HashMap<String, List<DocumentModel>>();
            List<String> diffItems = new ArrayList<String>();
            // boolean anyItemExistsInCurrentList = false;
            boolean multipleItems = false;
            int numExisting = 0;
            for (Statement itemStmt : itemStmts) {
                DocumentModel item = RelationHelper.getDocumentModel(
                        itemStmt.getSubject(), session);

                if (!isCorrectItemType(rootItem, item)) {
                    addWrongTypeItem(item);
                    continue;
                }

                if (currentItemList.contains(item.getVersionSeriesId())
                        && !diffItems.contains(item.getVersionSeriesId())) {
                    diffItems.add(item.getVersionSeriesId());
                    numExisting++;
                }

                updateRelatedItemsMap(relatedItems, item);
            }

            if (relatedItems.size() > 1) {
                multipleItems = true;
            }

            DocumentModel item;
            for (Map.Entry<String, List<DocumentModel>> entry : relatedItems.entrySet()) {
                item = getItemCorrectVersion(entry, session);
                if (multipleItems) {
                    EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                            directorStmt);
                    itemGlobalOrdering = maxItemGlobalOrdering
                            + stmtInfo.getOrdering();
                    DocItemRelation docItemRel = new DocItemRelation(doc, item,
                            directorStmt, itemGlobalOrdering);
                    if (numExisting == 1) {
                        if (currentItemList.contains(
                                item.getVersionSeriesId())) { // Mete el que
                                                              // existe a la
                                                              // lista final
                            docItemList.add(item.getVersionSeriesId());
                            updateItemData(item, directorStmt, null);
                        }
                    } else if (numExisting == 0) { // Mete todos de uno a uno en
                                                   // la lista para pintar en
                                                   // pantalla
                        docItemRelationList.add(docItemRel);
                    } else if (numExisting > 1) {
                        if (currentItemList.contains(
                                item.getVersionSeriesId())) { // Mete solo los
                                                              // que existen en
                                                              // la lista para
                                                              // pintar en
                                                              // pantalla
                            docItemRelationList.add(docItemRel);
                        }
                    }
                } else {
                    docItemList.add(item.getVersionSeriesId());
                    updateItemData(item, directorStmt, null);
                }
            }
        } else {
            missingItemList.add(doc);
        }
        return docItemList;
    }

    private DocumentModel getItemCorrectVersion(
            Map.Entry<String, List<DocumentModel>> entry, CoreSession session) {
        List<DocumentModel> itemList = entry.getValue();
        DocumentModel item;
        if (itemList.size() > 1) {
            if (itemList.contains(entry.getKey())) {
                // is related with wc
                item = session.getDocument(new IdRef(entry.getKey()));
            } else {
                DocumentModel itemWc = session.getDocument(
                        new IdRef(entry.getKey()));
                DocumentModel itemBv = EloraDocumentHelper.getBaseVersion(
                        itemWc);
                if (itemList.contains(itemBv.getId())) {
                    // is related with wc base version
                    item = itemBv;
                } else {
                    // get latest related
                    List<String> uidList = EloraDocumentHelper.getUidListFromDocList(
                            itemList);
                    Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                            itemList);
                    String type = itemList.get(0).getType();
                    item = EloraRelationHelper.getLatestRelatedVersion(session,
                            majorVersion, uidList, type);
                }
            }
        } else {
            item = itemList.get(0);
        }
        return item;
    }

    private void updateRelatedItemsMap(
            Map<String, List<DocumentModel>> relatedItems, DocumentModel item) {
        if (relatedItems.containsKey(item.getVersionSeriesId())) {
            relatedItems.get(item.getVersionSeriesId()).add(item);
        } else {
            List<DocumentModel> itemList = new ArrayList<DocumentModel>();
            itemList.add(item);
            relatedItems.put(item.getVersionSeriesId(), itemList);
        }
    }

    // private void addMultipleItemsToMap(DocumentModel doc, DocumentModel item)
    // {
    // if (multipleItemMap.containsKey(doc)) {
    // multipleItemMap.get(doc).add(item);
    // } else {
    // List<DocumentModel> docList = new ArrayList<DocumentModel>();
    // docList.add(item);
    // multipleItemMap.put(doc, docList);
    // }
    // }

    // public RelationData buildRelationData(DocumentModel item,
    // Statement directorStmt) {
    // EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
    // directorStmt);
    // // Integer quantity = Integer.parseInt(stmtInfo.getQuantity());
    // BigDecimal quantity = new BigDecimal(stmtInfo.getQuantity());
    // Integer ordering = stmtInfo.getOrdering();
    // // Boolean isManual = stmtInfo.getIsManual();
    // RelationData relationData = new RelationData(ordering, quantity, false,
    // item);
    // return relationData;
    // }

    public void updateItemData(DocumentModel item, Statement directorStmt,
            Integer newOrdering)
            throws CompositionWithMultipleVersionsException {

        EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                directorStmt);
        // Integer quantity = Integer.parseInt(stmtInfo.getQuantity());
        BigDecimal quantity = new BigDecimal(stmtInfo.getQuantity());
        int ordering = stmtInfo.getOrdering() != null ? stmtInfo.getOrdering()
                : 1;

        if (newOrdering == null) {
            itemGlobalOrdering = maxItemGlobalOrdering + ordering;
        } else {
            itemGlobalOrdering = newOrdering;
        }

        // Boolean isManual = stmtInfo.getIsManual();
        RelationData relationData = new RelationData(itemGlobalOrdering,
                directorOrdering, quantity, false, item);

        if (!directorItemDataMap.containsKey(item.getVersionSeriesId())) {
            directorItemDataMap.put(item.getVersionSeriesId(), relationData);
        } else {
            RelationData prevRelationData = directorItemDataMap.get(
                    item.getVersionSeriesId());
            if (prevRelationData.getObjItem().getId().equals(item.getId())) {
                prevRelationData.addQuantity(relationData.getQuantity());
                if (relationData.getDirectorOrdering() < prevRelationData.getDirectorOrdering()) {
                    prevRelationData.setOrdering(relationData.getOrdering());
                }
            } else {
                throw new CompositionWithMultipleVersionsException(
                        relationData.getObjItem());
            }
        }
    }

    private Set<String> getItemStructure(DocumentModel item,
            CoreSession session)
            throws CompositionWithMultipleVersionsException {
        currentItemDataMap = new HashMap<String, RelationData>();
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_COMPOSED_OF);
        List<Resource> predicateList = new ArrayList<Resource>();
        predicateList.add(predicateResource);
        List<Statement> itemStmts = EloraRelationHelper.getStatements(item,
                predicateList);

        Set<String> itemList = new HashSet<String>();
        for (Statement itemStmt : itemStmts) {
            DocumentModel childItem = RelationHelper.getDocumentModel(
                    itemStmt.getObject(), session);
            itemList.add(childItem.getVersionSeriesId());

            RelationData relationData = getRelationData(itemStmt, childItem);

            if (currentItemDataMap.containsKey(
                    childItem.getVersionSeriesId())) {
                throw new CompositionWithMultipleVersionsException(childItem);
            } else {
                currentItemDataMap.put(childItem.getVersionSeriesId(),
                        relationData);
            }
        }
        return itemList;
    }

    private RelationData getRelationData(Statement itemStmt,
            DocumentModel childItem) {
        EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(itemStmt);
        BigDecimal quantity = new BigDecimal(stmtInfo.getQuantity());
        Integer ordering = stmtInfo.getOrdering();
        Boolean isManual = stmtInfo.getIsManual();
        RelationData relationData = new RelationData(ordering, null, quantity,
                isManual, childItem);
        return relationData;
    }

    private boolean isCorrectItemType(DocumentModel rootItem,
            DocumentModel item) {
        try {
            checkItemType(rootItem, item);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private static void checkItemType(DocumentModel rootItem,
            DocumentModel item) throws EloraException {
        List<String> allowedTypes = getAllowedTypeForCurrentDoc(rootItem);
        if (!allowedTypes.contains(item.getType())) {
            throw new EloraException("No corresponding type found");
        }
    }

    private static List<String> getAllowedTypeForCurrentDoc(
            DocumentModel item) {
        List<String> allowedTypes = new ArrayList<String>();
        String docType = item.getType();
        switch (docType) {
        case EloraDoctypeConstants.BOM_PART:
        case EloraDoctypeConstants.BOM_PRODUCT:
            allowedTypes.add(EloraDoctypeConstants.BOM_PART);
            allowedTypes.add(EloraDoctypeConstants.BOM_PRODUCT);
            break;
        case EloraDoctypeConstants.BOM_PACKAGING:
            allowedTypes.add(EloraDoctypeConstants.BOM_PACKAGING);
            break;
        case EloraDoctypeConstants.BOM_TOOL:
            allowedTypes.add(EloraDoctypeConstants.BOM_TOOL);
            break;
        default:
            break;
        }
        return allowedTypes;
    }

    public class DocItemRelation implements Serializable {
        private static final long serialVersionUID = 1L;

        private DocumentModel doc;

        private DocumentModel item;

        private Statement directorStmt;

        private Integer itemGlobalOrdering;

        public DocumentModel getDoc() {
            return doc;
        }

        public DocumentModel getItem() {
            return item;
        }

        public Statement getDirectorStmt() {
            return directorStmt;
        }

        public Integer getItemGlobalOrdering() {
            return itemGlobalOrdering;
        }

        public DocItemRelation(DocumentModel doc, DocumentModel item,
                Statement directorStmt, Integer itemGlobalOrdering) {
            this.doc = doc;
            this.item = item;
            this.directorStmt = directorStmt;
            this.itemGlobalOrdering = itemGlobalOrdering;
        }
    }

}
