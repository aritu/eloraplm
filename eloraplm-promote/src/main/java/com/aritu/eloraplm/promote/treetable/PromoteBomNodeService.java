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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class PromoteBomNodeService implements PromoteNodeManager {

    private static final Log log = LogFactory.getLog(PromoteNodeService.class);

    protected CoreSession session;

    private EloraConfigTable hierarchicalRelationsConfig;

    private EloraConfigTable directRelationsConfig;

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

    public void setDirectRelationsConfig(EloraConfigTable directRelationsConfig) {
        this.directRelationsConfig = directRelationsConfig;
    }

    public PromoteBomNodeService(String finalState,
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
                                    // Check BOM related CAD documents
                                    resultMsg = checkRelatedDocs(doc,
                                            childFinalState);
                                    if (resultMsg.equals("")) {
                                        allOK = true;
                                    }
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

    private String checkRelatedDocs(DocumentModel doc, String docFinalState)
            throws EloraException {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        // Get all related Cad documents
        DocumentModelList objectDocList = RelationHelper.getObjectDocuments(
                doc, predicateResource);
        String resultMsg = "";
        String msg = "";
        for (DocumentModel objectDoc : objectDocList) {
            resultMsg = PromoteHelper.checkSupportedStates(doc, false,
                    docFinalState, objectDoc.getCurrentLifeCycleState(), null,
                    messages, session);
            if (!resultMsg.equals("")) {
                msg = messages.get("eloraplm.message.error.state.not.supported.by.related.cad");
                break;
            }
        }
        return msg;
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
        hierarchicalRelationsConfig = EloraConfigHelper.getBomHierarchicalRelationsConfig();
        directRelationsConfig = EloraConfigHelper.getBomDirectRelationsConfig();

        // Promote config
        relationDescendingPropagationConfig = EloraConfigHelper.getApproveDescendingPropagationConfig();

        lifeCycleStatesConfig = EloraConfigHelper.getLifecycleStatesConfig();
        finalStateOrdering = (long) lifeCycleStatesConfig.getRow(finalState).getProperty(
                "ordering");

        releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        // EloraConfigHelper.getReleasedLifecycleStatesConfig();

        // log.trace(logInitMsg
        // + "Hierarchical and direct cad relations configuration loaded.");
        // log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    public Map<String, List<String>> getIconOnlyRelationDocs(
            DocumentModel currentDoc) throws EloraException {
        return new HashMap<String, List<String>>();
    }

    @Override
    public EloraConfigTable getRelationDescendingPropagationConfig() {
        return relationDescendingPropagationConfig;
    }

}
