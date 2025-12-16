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
package com.aritu.eloraplm.bom.lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.treetable.BomCompositionListNodeService;
import com.aritu.eloraplm.bom.lists.treetable.ExternalBomListNodeService;
import com.aritu.eloraplm.bom.lists.util.BomListComparisonRowData;
import com.aritu.eloraplm.bom.treetable.BomCompositionNodeService;
import com.aritu.eloraplm.config.util.EloraPropertiesHelper;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.datatable.TableService;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeService;

/**
 *
 * @author aritu
 *
 */
public class BomListComparisonTableServiceImpl implements TableService {

    private List<RowData> bomListComparisonDataList;

    private Boolean differentStructure;

    @Override
    public List<RowData> getData(Object parentObject) {
        return null;
    }

    public List<RowData> getData(Object firstObject, Object secondObject,
            String bomListTypeFirstDoc, String bomListTypeSecondDoc,
            CoreSession session)
            throws EloraException, DocumentUnreadableException {

        differentStructure = false;
        bomListComparisonDataList = new ArrayList<RowData>();

        if (firstObject != null && secondObject != null
                && bomListTypeFirstDoc != null
                && bomListTypeSecondDoc != null) {
            compareBomLists((DocumentModel) firstObject,
                    (DocumentModel) secondObject, bomListTypeFirstDoc,
                    bomListTypeSecondDoc, session);
        }

        return bomListComparisonDataList;
    }

    private void compareBomLists(DocumentModel firstDoc,
            DocumentModel secondDoc, String bomListTypeFirstDoc,
            String bomListTypeSecondDoc, CoreSession session)
            throws EloraException, DocumentUnreadableException {

        TreeNode treeRoot1 = getBomListTree(firstDoc, bomListTypeFirstDoc,
                session);
        TreeNode treeRoot2 = getBomListTree(secondDoc, bomListTypeSecondDoc,
                session);

        calculateBomListComparisonResult(treeRoot1, treeRoot2);

    }

    private TreeNode getBomListTree(DocumentModel selectedDoc,
            String bomListType, CoreSession session)
            throws EloraException, DocumentUnreadableException {

        TreeNode docTree;

        if (bomListType.equals(BomListConstants.BOM_LIST_EBOM)) {
            RelationNodeService nodeService = new BomCompositionNodeService(
                    session, true, false, false);
            docTree = nodeService.getRoot(selectedDoc);
        } else if (EloraPropertiesHelper.areBomListsExternal()) {
            ExternalBomListNodeService nodeService = Framework.getService(
                    ExternalBomListNodeService.class);
            nodeService.init(session, 0,
                    EloraRelationConstants.DIRECTION_COMPOSITION, bomListType);
            docTree = nodeService.getRoot(selectedDoc);
        } else {
            BomCompositionListNodeService nodeService = new BomCompositionListNodeService(
                    session, bomListType);
            docTree = nodeService.getRoot(selectedDoc);
        }

        return docTree;
    }

    private void calculateBomListComparisonResult(TreeNode parentNode1,
            TreeNode parentNode2) {

        LinkedHashMap<String, List<TreeNode>> firstLevelChildrenData1 = getTreeNodeFirstChildrenData(
                parentNode1);
        LinkedHashMap<String, List<TreeNode>> firstLevelChildrenData2 = getTreeNodeFirstChildrenData(
                parentNode2);

        Set<String> refs2 = new LinkedHashSet<>(firstLevelChildrenData2.keySet());

        for (Map.Entry<String, List<TreeNode>> entry : firstLevelChildrenData1.entrySet()) {
            String ref = entry.getKey();

            if (refs2.contains(ref)) {
                bomListComparisonDataList.add(
                        compareTreeNodes(firstLevelChildrenData1.get(ref),
                                firstLevelChildrenData2.get(ref)));

                calculateBomListComparisonResult(
                        firstLevelChildrenData1.get(ref).get(0),
                        firstLevelChildrenData2.get(ref).get(0));
            } else {
                differentStructure = true;

                RelationNodeData nodeData1 = (RelationNodeData) firstLevelChildrenData1.get(
                        ref).get(0).getData();
                BomListComparisonRowData bomListComparisonRowData = (BomListComparisonRowData) createRowData(
                        nodeData1.getId(), nodeData1, null,
                        nodeData1.getLevel(), false, false, false);

                if (firstLevelChildrenData1.get(ref).size() > 1) {
                    bomListComparisonRowData.setMultipleFirstItems(true);
                }

                bomListComparisonDataList.add(bomListComparisonRowData);

                calculateBomListComparisonResult(
                        firstLevelChildrenData1.get(ref).get(0), null);
            }
        }

        // New docs in secondDoc
        Set<String> refs1 = firstLevelChildrenData1.keySet();
        if (!refs1.containsAll(refs2)) {
            differentStructure = true;
            List<String> newRefList = firstLevelChildrenData2.keySet().stream()
                    .filter(ref -> !refs1.contains(ref))
                    .collect(Collectors.toList());
            for (String docRef : newRefList) {
                // TODO: Si en la lista de nodeDatas hay varios habría que
                // utilizar un bucle y hacer lo de abajo para cada uno.
                RelationNodeData nodeData2 = (RelationNodeData) firstLevelChildrenData2.get(
                        docRef).get(0).getData();

                BomListComparisonRowData bomListComparisonRowData = (BomListComparisonRowData) createRowData(
                        nodeData2.getId(), null, nodeData2,
                        nodeData2.getLevel(), false, false, false);

                if (firstLevelChildrenData2.get(docRef).size() > 1) {
                    bomListComparisonRowData.setMultipleSecondItems(true);
                }

                bomListComparisonDataList.add(bomListComparisonRowData);

                calculateBomListComparisonResult(null,
                        firstLevelChildrenData2.get(docRef).get(0));
            }
        }
    }

    private BomListComparisonRowData compareTreeNodes(
            List<TreeNode> treeNodeList1, List<TreeNode> treeNodeList2) {
        // TODO: Toda la información está en el primer elemento. Solo
        // miramos si las listas tienen mas de un elemento para marcarlo
        // en la tabla

        RelationNodeData nodeData1 = (RelationNodeData) treeNodeList1.get(
                0).getData();
        RelationNodeData nodeData2 = (RelationNodeData) treeNodeList2.get(
                0).getData();

        BomListComparisonRowData bomListComparisonRowData = (BomListComparisonRowData) createRowData(
                nodeData1.getId(), nodeData1, nodeData2, nodeData1.getLevel(),
                false, false, false);

        if (treeNodeList1.size() > 1) {
            bomListComparisonRowData.setMultipleFirstItems(true);
        }
        if (treeNodeList2.size() > 1) {
            bomListComparisonRowData.setMultipleSecondItems(true);
        }

        // TODO: Falta aclarar el tema de las unidades para la comparación
        if (!nodeData1.getQuantity().equals(nodeData2.getQuantity())) {
            bomListComparisonRowData.setDiffQuantity(true);
        }
        /*
         * if (!nodeData1.getOrdering().equals(nodeData2.getOrdering())) {
         * bomListComparisonData.setDiffOrdering(true);
         * }
         */
        return bomListComparisonRowData;
    }

    private LinkedHashMap<String, List<TreeNode>> getTreeNodeFirstChildrenData(
            TreeNode parentNode) {
        // TODO: Como ahora sumamos los datos de los treenodes no sería
        // necesario una lista pero lo dejamos porque puede que haga falta
        // cuando nos digan cómo hacer la comparación. Además, sigo metiendo los
        // que se repiten en la lista para comprobar más tarde que hay más de
        // uno y marcarlo
        LinkedHashMap<String, List<TreeNode>> refData = new LinkedHashMap<String, List<TreeNode>>();
        if (parentNode != null) {
            // Create a list of children and sort them by ordering
            List<TreeNode> sortedChildren = new ArrayList<TreeNode>(parentNode.getChildren());
            sortedChildren.sort((node1, node2) -> {
                RelationNodeData nodeData1 = (RelationNodeData) node1.getData();
                RelationNodeData nodeData2 = (RelationNodeData) node2.getData();
                Integer ordering1 = nodeData1.getOrdering() != null ? nodeData1.getOrdering() : 0;
                Integer ordering2 = nodeData2.getOrdering() != null ? nodeData2.getOrdering() : 0;
                return Integer.compare(ordering1, ordering2);
            });

            for (TreeNode node : sortedChildren) {
                RelationNodeData nodeData = (RelationNodeData) node.getData();

                String childRef;
                if (!nodeData.getIsExternalSource()) {
                    childRef = nodeData.getData().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                } else {
                    childRef = nodeData.getBomListExternalData().getReference();
                }

                if (refData.containsKey(childRef)) {
                    // TODO: Por ahora sumamos las cantidades sin darle ninguna
                    // importancia al orden. Tampocomo vamos a diferenciar si
                    // comparamos EBOMvsMBOM o MBOMvsMBOM
                    addQuantity(refData, nodeData, childRef);

                    // TODO: Aunque se sume la cantidad sigo metiendo el
                    // treeNode en la lista para poder marcar luego que hay mas
                    // de uno. Pero el que tiene la información buena va a ser
                    // el primero.
                    refData.get(childRef).add(node);
                } else {
                    List<TreeNode> list = new ArrayList<TreeNode>();
                    list.add(node);
                    refData.put(childRef, list);
                }
            }
        }
        return refData;
    }

    private void addQuantity(Map<String, List<TreeNode>> refData,
            RelationNodeData nodeData, String childRef) {
        RelationNodeData prevNodeData = (RelationNodeData) refData.get(
                childRef).get(0).getData();
        BigDecimal prevQuantity = new BigDecimal(prevNodeData.getQuantity());
        BigDecimal newQuantity = new BigDecimal(nodeData.getQuantity());
        String sumQuantity = prevQuantity.add(newQuantity).toString();
        prevNodeData.setQuantity(sumQuantity);
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, null, null, 0, false, false, false);
    }

    public RowData createRowData(String rowId, RelationNodeData nodeData1,
            RelationNodeData nodeData2, int level, boolean isNew,
            boolean isModified, boolean isRemoved) {
        RowData row = new BomListComparisonRowData(rowId, nodeData1, nodeData2,
                level, isNew, isModified, isRemoved);
        return row;
    }

    public Boolean getDifferentStructure() {
        return differentStructure;
    }

}
