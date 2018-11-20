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
package com.aritu.eloraplm.promote.checker.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.config.util.LifecyclesConfig;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.promote.treetable.PromoteNodeData;
import com.aritu.eloraplm.promote.treetable.NodeDynamicInfo;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.constants.PromoteConstants;
import com.aritu.eloraplm.promote.constants.util.PromoteHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomPromoteCheckerService extends PromoteCheckerService {

    protected EloraConfigTable lifeCycleStatesConfig;

    protected Map<String, Boolean> iconOnlyPredicates;

    protected RelationManager relationManager = Framework.getLocalService(
            RelationManager.class);

    // protected EloraConfigTable relationDescendingPropagationConfig;

    private List<Resource> hierarchicalAndDirectPredicates;

    private List<String> directPredicates;

    private List<Resource> specialPredicates;

    @Override
    public List<Resource> getHierarchicalAndDirectPredicates() {
        return hierarchicalAndDirectPredicates;
    }

    @Override
    public EloraConfigTable getLifeCycleStatesConfig() {
        return lifeCycleStatesConfig;
    }

    @Override
    public List<Resource> getSpecialPredicates() {
        return specialPredicates;
    }

    @Override
    public List<String> getDirectPredicates() {
        return directPredicates;
    }

    public BomPromoteCheckerService() throws EloraException {
        loadConfigurations();
    }

    @Override
    public void processNodeInfo(DocumentModel doc,
            PromoteNodeData parentNodeData, NodeDynamicInfo nodeInfo, int level,
            boolean isSpecial, String parentState, Statement stmt,
            String promoteTransition, String finalState,
            Map<String, String> messages) throws EloraException {

        String childCurrentState = doc.getCurrentLifeCycleState();
        String childFinalState = getChildFinalState(nodeInfo.getIsPropagated(),
                childCurrentState, finalState);

        String result = PromoteConstants.RESULT_KO;
        String resultMsg = "";
        if (isFirstLevel(level) || (parentNodeData.getIsPropagated()
                && isResultOK(parentNodeData))) {
            if (nodeInfo.getIsPropagated()) {
                if (isSpecialDocumentCausingKO(nodeInfo)) {
                    resultMsg = nodeInfo.getHiddenResultMsg();
                } else {
                    resultMsg = getCheckResultMsg(doc,
                            nodeInfo.getIsPropagated(), parentNodeData,
                            isSpecial, parentState, stmt, promoteTransition,
                            childFinalState, level, messages);
                }
            } else {
                resultMsg = PromoteHelper.checkSupportedStates(doc,
                        nodeInfo.getIsPropagated(), parentNodeData.getDocId(),
                        isSpecial, parentState, promoteTransition,
                        hierarchicalAndDirectPredicates, childFinalState, stmt,
                        messages);
                // if (!isFirstLevel(level)) {
                // nodeInfo.setIsPropagated(false);
                // nodeInfo.setIsEnforced(false);
                // }
            }
        } else {
            if (nodeInfo.getIsPropagated()) {
                if (isSpecialDocumentCausingKO(nodeInfo)) {
                    resultMsg = nodeInfo.getHiddenResultMsg();
                } else {
                    resultMsg = getCheckResultMsg(doc,
                            nodeInfo.getIsPropagated(), parentNodeData,
                            isSpecial, parentState, stmt, promoteTransition,
                            childFinalState, level, messages);
                }
            }
        }

        // if (resultMsg.equals("")) {
        // result = PromoteConstants.RESULT_OK;
        // nodeInfo.setFinalState(childFinalState);
        // } else if (!isFirstLevel(level)) {
        // nodeInfo.setFinalState(childCurrentState);
        // } else {
        // nodeInfo.setFinalState(finalState);
        // }

        if (resultMsg.equals("")) {
            result = PromoteConstants.RESULT_OK;
        } else {
            topLevelOK = false;
        }
        nodeInfo.setFinalState(childFinalState);

        boolean alreadyPromoted = PromoteHelper.isAlreadyPromoted(doc,
                finalState, lifeCycleStatesConfig);
        nodeInfo.setAlreadyPromoted(alreadyPromoted);
        nodeInfo.setResultMsg(resultMsg);
        nodeInfo.setResult(result);
    }

    private String getChildFinalState(boolean isPropagated,
            String childCurrentState, String finalState) {
        String childFinalState;
        if (isPropagated) {
            childFinalState = finalState;
        } else {
            childFinalState = childCurrentState;
        }
        return childFinalState;
    }

    private boolean isSpecialDocumentCausingKO(NodeDynamicInfo nodeInfo) {
        String hiddenResult = nodeInfo.getHiddenResult() == null ? ""
                : nodeInfo.getHiddenResult();
        return hiddenResult.equals(PromoteConstants.RESULT_KO);
    }

    private boolean isResultOK(PromoteNodeData nodeData) {
        return nodeData.getResult().equals(PromoteConstants.RESULT_OK);
    }

    private boolean isFirstLevel(int level) {
        return level == 1 || level == 0;
    }

    private String getCheckResultMsg(DocumentModel doc, boolean isPropagated,
            PromoteNodeData parentNodeData, boolean isSpecial,
            String parentState, Statement stmt, String promoteTransition,
            String childFinalState, int level, Map<String, String> messages)
            throws EloraException {
        String resultMsg;
        CoreSession session = doc.getCoreSession();
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        if (session.hasPermission(doc.getRef(), SecurityConstants.WRITE)) {
            if (!wcDoc.isCheckedOut()) {
                if (PromoteHelper.checkDocumentLock(wcDoc)) {
                    if (PromoteHelper.checkAllowedTransition(doc,
                            promoteTransition)) {
                        String parentDocId = null;
                        if (!isFirstLevel(level)) {
                            parentDocId = parentNodeData.getDocId();
                        }
                        resultMsg = PromoteHelper.checkSupportedStates(doc,
                                isPropagated, parentDocId, isSpecial,
                                parentState, promoteTransition,
                                hierarchicalAndDirectPredicates,
                                childFinalState, stmt, messages);
                        if (resultMsg.equals("")) {
                            if (PromoteHelper.checkReleasedAndObsoleteInMajor(
                                    doc, session)) {
                                if (!PromoteHelper.isTransitionAllowsConfig(
                                        promoteTransition)) {
                                    resultMsg = checkRelatedDocs(doc,
                                            childFinalState, messages);
                                }
                            } else {
                                resultMsg = messages.get(
                                        "eloraplm.message.promote.error.released.in.major");
                            }
                        }
                    } else {
                        resultMsg = messages.get(
                                "eloraplm.message.error.transition.not.allowed");
                    }
                } else {
                    resultMsg = messages.get("Document locked by "
                            + wcDoc.getLockInfo().getOwner());
                }
            } else {
                resultMsg = messages.get(
                        "eloraplm.message.error.promote.documentCheckedOut");
            }
        } else {
            resultMsg = messages.get(
                    "eloraplm.message.error.promote.noWritePermisssion");
        }
        return resultMsg;
    }

    private String checkRelatedDocs(DocumentModel doc, String docFinalState,
            Map<String, String> messages) throws EloraException {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        DocumentModelList objectDocList = RelationHelper.getObjectDocuments(doc,
                predicateResource);
        String msg = "";
        for (DocumentModel objectDoc : objectDocList) {
            if (!EloraDocumentHelper.isSupported(docFinalState,
                    objectDoc.getCurrentLifeCycleState())) {
                msg = messages.get(
                        "eloraplm.message.error.state.not.supported.by.related.cad");
                break;
            }
        }
        return msg;
    }

    protected void loadConfigurations() throws EloraException {
        // TODO Hau konfiguraziotik bete!!
        iconOnlyPredicates = new LinkedHashMap<String, Boolean>();
        iconOnlyPredicates.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                false);

        setDirectPredicates();
        setSpecialPredicates();
        setHierarchicalAndDirectPredicates();

        // Hierarchical and direct relations
        // hierarchicalRelationsConfig =
        // new ArrayList<>(RelationsConfig.bomHierarchicalRelationsConfig);
        // directRelationsConfig = new
        // ArrayList<>(RelationsConfig.bomDirectRelationsConfig);

        lifeCycleStatesConfig = LifecyclesConfig.allStatesConfig;

        //
        // finalStateOrdering = (long)
        // lifeCycleStatesConfig.getRow(finalState).getProperty(
        // "ordering");

        // releasedStatesConfig = LifecyclesConfig.releasedStatesConfig;

        // log.trace(logInitMsg
        // + "Hierarchical and direct cad relations configuration loaded.");
        // log.trace(logInitMsg + "--- EXIT ---");
    }

    private void setDirectPredicates() {
        directPredicates = new ArrayList<>(
                RelationsConfig.bomDirectRelationsList);
    }

    private void setSpecialPredicates() {
        specialPredicates = new ArrayList<Resource>();
    }

    private void setHierarchicalAndDirectPredicates() {
        List<String> hierarchicalAndDirectPredicateList = new ArrayList<String>();
        hierarchicalAndDirectPredicateList.addAll(
                RelationsConfig.bomHierarchicalRelationsList);
        hierarchicalAndDirectPredicateList.addAll(
                RelationsConfig.bomDirectRelationsList);
        hierarchicalAndDirectPredicates = getPredicateResourceList(
                hierarchicalAndDirectPredicateList);
    }

    private List<Resource> getPredicateResourceList(
            List<String> hierarchicalAndDirectPredicateList) {
        List<Resource> resourceList = new ArrayList<Resource>();
        for (String hierarchicalAndDirectPredicate : hierarchicalAndDirectPredicateList) {
            Resource predicateResource = new ResourceImpl(
                    hierarchicalAndDirectPredicate);
            resourceList.add(predicateResource);
        }
        return resourceList;
    }

    @Override
    public Map<String, List<String>> getIconOnlyRelationDocs(
            DocumentModel currentDoc) throws EloraException {
        return new HashMap<String, List<String>>();
    }
}
