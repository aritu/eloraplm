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
package com.aritu.eloraplm.bom.relations;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
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
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.relations.EloraBasicRelationBean;

@Name("specificationRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class SpecificationRelationBean extends EloraBasicRelationBean implements
        Serializable {

    private static final long serialVersionUID = 1L;

    protected List<Statement> outgoingSpecificationStatements;

    protected List<Statement> incomingSpecificationStatements;

    protected List<StatementInfo> outgoingSpecificationStatementsInfo;

    protected List<StatementInfo> incomingSpecificationStatementsInfo;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public SpecificationRelationBean() {
    }

    @Factory(value = "outgoingSpecificationRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingSpecificationStatementsInfo() {
        if (outgoingSpecificationStatementsInfo != null) {
            return outgoingSpecificationStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(currentDoc.getRef());
        }
        Resource predicate = new QNameResourceImpl(
                EloraRelationConstants.BOM_HAS_SPECIFICATION, "");
        outgoingSpecificationStatements = RelationHelper.getStatements(
                currentDoc, predicate);

        if (outgoingSpecificationStatements.isEmpty()) {
            outgoingSpecificationStatements = Collections.emptyList();
            outgoingSpecificationStatementsInfo = Collections.emptyList();
        } else {
            outgoingSpecificationStatementsInfo = eloraRelationActions.getStatementsInfo(outgoingSpecificationStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(outgoingSpecificationStatementsInfo, comp);
        }
        return outgoingSpecificationStatementsInfo;
    }

    @Factory(value = "incomingSpecificationRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingSpecificationStatementsInfo() {
        if (incomingSpecificationStatementsInfo != null) {
            return incomingSpecificationStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(currentDoc.getRef());
        }
        Resource predicate = new QNameResourceImpl(
                EloraRelationConstants.BOM_HAS_SPECIFICATION, "");
        incomingSpecificationStatements = EloraRelationHelper.getSubjectStatements(
                currentDoc, predicate);

        if (incomingSpecificationStatements.isEmpty()) {
            incomingSpecificationStatements = Collections.emptyList();
            incomingSpecificationStatementsInfo = Collections.emptyList();
        } else {
            incomingSpecificationStatementsInfo = eloraRelationActions.getStatementsInfo(incomingSpecificationStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(incomingSpecificationStatementsInfo, comp);
        }
        return incomingSpecificationStatementsInfo;
    }

    @Override
    public String addRelation() {
        resetEventContext();
        super.setPredicateUri(EloraRelationConstants.BOM_HAS_SPECIFICATION);
        super.addRelation();
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
            evtCtx.remove("outgoingSpecificationRelations");
        }
    }

    protected void resetStatements() {
        outgoingSpecificationStatements = null;
        outgoingSpecificationStatementsInfo = null;
        incomingSpecificationStatements = null;
        incomingSpecificationStatementsInfo = null;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }

}