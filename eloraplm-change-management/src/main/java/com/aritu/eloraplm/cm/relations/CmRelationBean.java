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
package com.aritu.eloraplm.cm.relations;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.CMDocTypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.relations.EloraBasicRelationBean;

@Name("cmRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmRelationBean extends EloraBasicRelationBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    protected List<Statement> outgoingCmStatements;

    protected List<Statement> incomingCmStatements;

    protected List<StatementInfo> outgoingCmStatementsInfo;

    protected List<StatementInfo> incomingCmStatementsInfo;

    protected List<StatementInfo> cmStatementsInfo;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public CmRelationBean() {
    }

    @Factory(value = "outgoingCmRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingCmStatementsInfo() {
        if (outgoingCmStatementsInfo != null) {
            return outgoingCmStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        Resource predicate = new ResourceImpl(
                EloraRelationConstants.CM_PROCESS_IS_MANAGED_IN);
        outgoingCmStatements = RelationHelper.getStatements(currentDoc,
                predicate);

        if (outgoingCmStatements.isEmpty()) {
            outgoingCmStatements = Collections.emptyList();
            outgoingCmStatementsInfo = Collections.emptyList();
        } else {
            outgoingCmStatementsInfo = eloraRelationActions.getStatementsInfo(
                    outgoingCmStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingCmStatementsInfo, comp);
        }
        return outgoingCmStatementsInfo;
    }

    @Factory(value = "incomingCmRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingCmStatementsInfo() {
        if (incomingCmStatementsInfo != null) {
            return incomingCmStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        Resource predicate = new ResourceImpl(
                EloraRelationConstants.CM_PROCESS_IS_MANAGED_IN);
        incomingCmStatements = EloraRelationHelper.getSubjectStatements(
                currentDoc, predicate);

        if (incomingCmStatements.isEmpty()) {
            incomingCmStatements = Collections.emptyList();
            incomingCmStatementsInfo = Collections.emptyList();
        } else {
            incomingCmStatementsInfo = eloraRelationActions.getStatementsInfo(
                    incomingCmStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(incomingCmStatementsInfo, comp);
        }
        return incomingCmStatementsInfo;
    }

    @Factory(value = "cmRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getCmStatementsInfo() {
        if (cmStatementsInfo != null && !cmStatementsInfo.isEmpty()) {
            return cmStatementsInfo;
        }

        outgoingCmStatementsInfo = getOutgoingCmStatementsInfo();
        incomingCmStatementsInfo = getIncomingCmStatementsInfo();

        cmStatementsInfo = new ArrayList<StatementInfo>(
                incomingCmStatementsInfo);
        cmStatementsInfo.addAll(outgoingCmStatementsInfo);

        return cmStatementsInfo;
    }

    // public boolean hasRelatedDocumentPermission(StatementInfo stmtInfo,
    // String permission) {
    //
    // NodeInfo subjectInfo = stmtInfo.getSubjectInfo();
    // NodeInfo objectInfo = stmtInfo.getSubjectInfo();
    // DocumentModel subjectDoc = stmtInfo.getSubjectInfo().getDocumentModel();
    // if (subjectInfo.isDocumentVisible() && objectInfo.isDocumentVisible()) {
    // if (subjectDoc.getId() == getCurrentDocument().getId()) {
    // DocumentModel objectDoc = stmtInfo.getObjectInfo().getDocumentModel();
    // return documentManager.hasPermission(objectDoc.getRef(),
    // permission);
    // } else {
    // return documentManager.hasPermission(subjectDoc.getRef(),
    // permission);
    // }
    // } else {
    // return false;
    // }
    // }

    public boolean isRelatedDocumentVisible(StatementInfo stmtInfo) {
        DocumentModel subject = stmtInfo.getSubjectInfo().getDocumentModel();
        // Subject is null when user has not permission
        if (subject != null
                && subject.getId() == getCurrentDocument().getId()) {
            return stmtInfo.getObjectInfo().isDocumentVisible();
        } else {
            return stmtInfo.getSubjectInfo().isDocumentVisible();
        }
    }

    public NodeInfo getRelatedDocumentInfo(StatementInfo stmtInfo) {
        DocumentModel subject = stmtInfo.getSubjectInfo().getDocumentModel();

        // Subject is null when user has not permission
        if (subject != null
                && subject.getId() == getCurrentDocument().getId()) {
            return stmtInfo.getObjectInfo();
        } else {
            return stmtInfo.getSubjectInfo();
        }
    }

    @Override
    public String addRelation() {
        resetEventContext();
        super.setPredicateUri(EloraRelationConstants.CM_PROCESS_IS_MANAGED_IN);

        DocumentRef docRef = new IdRef(super.getObjectDocumentUid());
        String objType = documentManager.getDocument(docRef).getType();
        if (objType.equals(CMDocTypeConstants.CM_PR)) {
            super.addRelation(true);
        } else {
            super.addRelation();
        }

        resetStatements();
        return null;
    }

    @Override
    public String deleteStatement(StatementInfo stmtInfo) {
        resetEventContext();
        super.deleteStatement(stmtInfo);
        resetStatements();
        return null;
    }

    protected void resetEventContext() {
        Context evtCtx = Contexts.getEventContext();
        if (evtCtx != null) {
            evtCtx.remove("outgoingCmRelations");
        }
    }

    protected void resetStatements() {
        outgoingCmStatements = null;
        outgoingCmStatementsInfo = null;
        incomingCmStatements = null;
        incomingCmStatementsInfo = null;
        cmStatementsInfo = null;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }

}