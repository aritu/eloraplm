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
package com.aritu.eloraplm.bom.autostructure;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.bom.autostructure.util.ItemStructureData;
import com.aritu.eloraplm.bom.autostructure.util.ItemStructureData.DocItemRelation;
import com.aritu.eloraplm.bom.autostructure.util.RelationData;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.aritu.eloraplm.exceptions.CompositionWithMultipleVersionsException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ItemStructureUpdater {

    private static final Log log = LogFactory.getLog(
            ItemStructureUpdater.class);

    public static void updateStructure(ItemStructureData itemStructureData,
            List<DocItemRelation> selectedDocItemRelations,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session)
            throws CompositionWithMultipleVersionsException {

        addSelectedNewItems(itemStructureData, selectedDocItemRelations);

        addDirectorsNewItems(itemStructureData, eloraDocumentRelationManager,
                session);

        removeCurrentLeftoverItems(itemStructureData,
                eloraDocumentRelationManager, session);
        updateCommonItems(itemStructureData, eloraDocumentRelationManager,
                session);
    }

    private static void addSelectedNewItems(ItemStructureData itemStructureData,
            List<DocItemRelation> selectedDocItemRelations)
            throws CompositionWithMultipleVersionsException {
        for (DocItemRelation docItemRelation : selectedDocItemRelations) {
            itemStructureData.getDirectorItemList().add(
                    docItemRelation.getItem().getVersionSeriesId());
            itemStructureData.updateItemData(docItemRelation.getItem(),
                    docItemRelation.getDirectorStmt(),
                    docItemRelation.getItemGlobalOrdering());
        }

    }

    private static void addDirectorsNewItems(
            ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {

        Set<String> currentItemList = itemStructureData.getCurrentItemList();
        Set<String> newItemList = itemStructureData.getDirectorItemList();

        if (!currentItemList.containsAll(newItemList)) {
            // there are new elements
            Set<String> itemsToAdd = new HashSet<String>(newItemList);
            itemsToAdd.removeAll(currentItemList);
            for (String itemId : itemsToAdd) {
                addNewItem(itemId, itemStructureData,
                        eloraDocumentRelationManager, session);
            }
        }
    }

    private static void addNewItem(String itemId,
            ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        RelationData relData = itemStructureData.getDirectorItemDataMap().get(
                itemId);
        DocumentModel newItem = relData.getObjItem();
        DocumentModel rootItem = itemStructureData.getRootItem();

        eloraDocumentRelationManager.addRelation(session, rootItem, newItem,
                EloraRelationConstants.BOM_COMPOSED_OF, "",
                EloraDecimalHelper.fromDecimalToStandard(relData.getQuantity()),
                relData.getOrdering());

        itemStructureData.setIsStructureUpdated(true);
        log.trace("Relation added to item |" + newItem.getId() + "|");
    }

    private static void removeCurrentLeftoverItems(
            ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        Set<String> currentItemList = itemStructureData.getCurrentItemList();
        Set<String> newItemList = itemStructureData.getDirectorItemList();
        if (!newItemList.containsAll(currentItemList)) {
            // there are current elements to remove
            Set<String> itemsToRemove = new HashSet<String>(currentItemList);
            itemsToRemove.removeAll(newItemList);
            for (String itemId : itemsToRemove) {
                removeItem(itemId, itemStructureData,
                        eloraDocumentRelationManager, session);
            }
        }
    }

    private static void removeItem(String itemId,
            ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        RelationData curRelationData = itemStructureData.getCurrentItemDataMap().get(
                itemId);
        Boolean isManual = curRelationData.getIsManual();
        if (isManual == null || !isManual) {
            eloraDocumentRelationManager.softDeleteRelation(session,
                    itemStructureData.getRootItem(),
                    EloraRelationConstants.BOM_COMPOSED_OF,
                    curRelationData.getObjItem());
            itemStructureData.setIsStructureUpdated(true);
            log.trace("Relation removed to item |"
                    + curRelationData.getObjItem().getId() + "|");
        }
    }

    private static void updateCommonItems(ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {

        Set<String> currentItemList = itemStructureData.getCurrentItemList();
        Set<String> newItemList = itemStructureData.getDirectorItemList();

        Set<String> commonItemList = new HashSet<String>(newItemList);
        commonItemList.retainAll(currentItemList);
        for (String itemId : commonItemList) {
            updateItemProperties(itemId, itemStructureData,
                    eloraDocumentRelationManager, session);
        }
    }

    private static void updateItemProperties(String itemId,
            ItemStructureData itemStructureData,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        RelationData newRelationData = itemStructureData.getDirectorItemDataMap().get(
                itemId);
        RelationData curRelationData = itemStructureData.getCurrentItemDataMap().get(
                itemId);

        Boolean isManual = curRelationData.getIsManual();
        if ((isManual == null || !isManual) && relationPropertiesHaveChanged(
                newRelationData, curRelationData)) {
            eloraDocumentRelationManager.updateRelation(session,
                    itemStructureData.getRootItem(),
                    EloraRelationConstants.BOM_COMPOSED_OF,
                    curRelationData.getObjItem(), newRelationData.getObjItem(),
                    EloraDecimalHelper.fromDecimalToStandard(
                            newRelationData.getQuantity()),
                    newRelationData.getOrdering(), null, null, null, null);

            itemStructureData.setIsStructureUpdated(true);
            log.trace("Relation updated to item |"
                    + curRelationData.getObjItem().getId() + "|");
        }
    }

    private static boolean relationPropertiesHaveChanged(
            RelationData newRelationData, RelationData curRelationData) {
        BigDecimal newQty = newRelationData.getQuantity();
        BigDecimal curQty = curRelationData.getQuantity();
        Integer newOrd = newRelationData.getOrdering();
        Integer curOrd = curRelationData.getOrdering();
        DocumentModel newItem = newRelationData.getObjItem();
        DocumentModel curItem = curRelationData.getObjItem();
        if (newQty == null) {
            newQty = new BigDecimal(0);
        }
        if (curQty == null) {
            curQty = new BigDecimal(0);
        }
        if (newOrd == null) {
            newOrd = 0;
        }
        if (curOrd == null) {
            curOrd = 0;
        }

        return (newQty.floatValue() != curQty.floatValue())
                || (newOrd.intValue() != curOrd.intValue())
                || (newItem.getId() != curItem.getId());
    }

}
