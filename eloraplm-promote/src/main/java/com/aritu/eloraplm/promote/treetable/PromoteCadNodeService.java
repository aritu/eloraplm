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
package com.aritu.eloraplm.promote.treetable;

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

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.util.PromoteConstants;
import com.aritu.eloraplm.promote.util.PromoteHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class PromoteCadNodeService implements PromoteNodeManager {

    protected CoreSession session;

    protected EloraConfigTable hierarchicalRelationsConfig;

    protected EloraConfigTable directRelationsConfig;

    protected EloraConfigTable lifeCycleStatesConfig;

    protected EloraConfigTable releasedStatesConfig;

    protected Map<String, String> messages;

    protected String finalState;

    protected long finalStateOrdering;

    protected Map<String, Boolean> iconOnlyPredicates;

    protected RelationManager relationManager = Framework.getLocalService(RelationManager.class);

    protected EloraConfigTable relationDescendingPropagationConfig;

    @Override
    public EloraConfigTable getHierarchicalRelationsConfig() {
        return hierarchicalRelationsConfig;
    }

    @Override
    public EloraConfigTable getDirectRelationsConfig() {
        return directRelationsConfig;
    }

    @Override
    public EloraConfigTable getRelationDescendingPropagationConfig() {
        return relationDescendingPropagationConfig;
    }

    public PromoteCadNodeService(String finalState,
            Map<String, String> messages, CoreSession session)
            throws EloraException {
        this.session = session;
        this.messages = messages;
        this.finalState = finalState;
        loadConfigurations();
    }

    @Override
    public void processNodeInfo(DocumentModel doc, NodeDynamicInfo nodeInfo,
            int level, boolean isSpecial, String parentState, Statement stmt,
            String promoteTransition) throws EloraException {
        String childFinalState;
        String childCurrentState = doc.getCurrentLifeCycleState();
        if (nodeInfo.getIsPropagated()) {
            childFinalState = finalState;
        } else {
            childFinalState = childCurrentState;
        }

        boolean alreadyPromoted = PromoteHelper.calculateAlreadyPromoted(doc,
                finalStateOrdering, lifeCycleStatesConfig);
        String result = PromoteConstants.RESULT_KO;
        String resultMsg = "";
        if (!alreadyPromoted) {
            boolean allOK = false;
            // Promote is propagated to docs with state ordering minor
            // than final state ordering (e.g. preliminary < approved)
            DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());

            if (session.hasPermission(doc.getRef(), SecurityConstants.WRITE)) {
                if (!wcDoc.isCheckedOut()) {
                    if (PromoteHelper.checkDocumentLock(wcDoc, session)) {
                        // If it is not locked or is locked by user (admin)
                        // Check if final state is possible by transition
                        if (PromoteHelper.checkAllowedTransition(doc,
                                promoteTransition)) {
                            // Check supported states
                            resultMsg = PromoteHelper.checkSupportedStates(doc,
                                    isSpecial, parentState, childFinalState,
                                    stmt, messages, session);
                            if (resultMsg.equals("")) {
                                // Check if there is no released doc in the same
                                // major
                                if (PromoteHelper.checkReleasedInMajor(doc,
                                        session)) {
                                    allOK = true;
                                } else {
                                    // You chose a doc version that is not
                                    // released and exists another version
                                    // released in the same major version
                                    resultMsg = messages.get("eloraplm.message.promote.error.released.in.major");
                                }
                            }
                        } else {
                            resultMsg = messages.get("eloraplm.message.error.transition.not.allowed");
                        }
                    } else {
                        resultMsg = messages.get("Document locked by "
                                + wcDoc.getLockInfo().getOwner());
                    }
                } else {
                    resultMsg = messages.get("eloraplm.message.error.doc.checked.out");
                }
            } else {
                resultMsg = messages.get("eloraplm.message.error.no.write.permission");
            }

            if (allOK) {
                // If everything goes ok we show final status and OK
                result = PromoteConstants.RESULT_OK;
                nodeInfo.setFinalState(childFinalState);
            } else if (level != 1 && level != 0) {
                nodeInfo.setFinalState(childCurrentState);
            } else {
                nodeInfo.setFinalState(finalState);
            }

        } else {
            // If ordering is equal or major check if child state is
            // supported
            resultMsg = PromoteHelper.checkSupportedStates(doc, isSpecial,
                    parentState, doc.getCurrentLifeCycleState(), stmt,
                    messages, session);
            if (resultMsg.equals("")) {
                result = PromoteConstants.RESULT_OK;
            } else {
                // E.g. A document could be obsolete (released) but parent
                // doesn't support this state. This must be KO.
                result = PromoteConstants.RESULT_KO;
            }
            // In this case it is not necessary to promote this doc so we
            // marc propagated = false and enforced = true. This way, when
            // we run promote process is enough to check isPropagated to
            // know which documents to promote
            if (level != 1 && level != 0) {
                nodeInfo.setIsPropagated(false);
                nodeInfo.setIsEnforced(true);
                nodeInfo.setFinalState(childCurrentState);
            }
        }

        nodeInfo.setAlreadyPromoted(alreadyPromoted);
        nodeInfo.setResultMsg(resultMsg);
        nodeInfo.setResult(result);
    }

    // TODO: Hau RelationNodeService barruan dago baina parametrua pasau barik!!
    /**
     * Gets the UIDs of the documents that have an iconOnly relation with the
     * current document
     *
     * @param nodeData
     * @return
     * @throws EloraException
     */
    @Override
    public Map<String, List<String>> getIconOnlyRelationDocs(
            DocumentModel currentDoc) throws EloraException {
        Map<String, List<String>> iconOnlyRelationDocs = new HashMap<String, List<String>>();

        for (Map.Entry<String, Boolean> predicateEntry : iconOnlyPredicates.entrySet()) {
            String predicateUri = predicateEntry.getKey();
            Boolean isSpecial = predicateEntry.getValue();

            Resource predicateResource = new ResourceImpl(predicateUri);

            DocumentModelList relatedDocs;
            if (isSpecial) {
                relatedDocs = RelationHelper.getSubjectDocuments(
                        predicateResource, currentDoc);
            } else {
                relatedDocs = RelationHelper.getObjectDocuments(currentDoc,
                        predicateResource);
            }

            if (!relatedDocs.isEmpty()) {
                for (DocumentModel doc : relatedDocs) {
                    String docId = doc.getId();
                    if (iconOnlyRelationDocs.containsKey(docId)) {
                        iconOnlyRelationDocs.get(docId).add(predicateUri);
                    } else {
                        List<String> relations = new ArrayList<String>();
                        relations.add(predicateUri);
                        iconOnlyRelationDocs.put(doc.getId(), relations);
                    }
                }
            }
        }
        return iconOnlyRelationDocs;
    }

    protected void loadConfigurations() throws EloraException {
        // String logInitMsg = "[loadConfigurations] ["
        // + session.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        // TODO Hau konfiguraziotik bete!!
        iconOnlyPredicates = new LinkedHashMap<String, Boolean>();
        iconOnlyPredicates.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                false);

        // Hierarchical and direct relations
        hierarchicalRelationsConfig = EloraConfigHelper.getCadHierarchicalRelationsConfig();
        directRelationsConfig = EloraConfigHelper.getCadDirectRelationsConfig();

        // Promote config
        relationDescendingPropagationConfig = EloraConfigHelper.getApproveDescendingPropagationConfig();

        lifeCycleStatesConfig = EloraConfigHelper.getLifecycleStatesConfig();
        finalStateOrdering = (long) lifeCycleStatesConfig.getRow(finalState).getProperty(
                "ordering");

        releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();

        // log.trace(logInitMsg
        // + "Hierarchical and direct cad relations configuration loaded.");
        // log.trace(logInitMsg + "--- EXIT ---");
    }

}
