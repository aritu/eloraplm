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
package com.aritu.eloraplm.pdm.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class RelationSwitchHelper {

    private static final String CHECKOUT_SWITCH_CHILDREN_OPTION_AS_STORED = "AsStored";

    private static final String CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_VERSIONS = "LatestVersions";

    private static final String CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_RELEASED = "LatestReleased";

    private static final String CHECKOUT_SWITCH_CHILDREN_OPTION_WORKING_COPIES = "WorkingCopies";

    private static final Log log = LogFactory.getLog(
            RelationSwitchHelper.class);

    public static void switchRelations(CoreSession session,
            EloraDocumentRelationManager edrm, DocumentModel subjectWcDoc)
            throws EloraException {

        switchRelations(session, edrm, subjectWcDoc, null);
    }

    public static void switchRelations(CoreSession session,
            EloraDocumentRelationManager edrm, DocumentModel subjectWcDoc,
            List<String> filteredPredicates) throws EloraException {

        String logInitMsg = "[switchRelations] ["
                + session.getPrincipal().getName() + "] ";

        List<String> switchablePredicates = new ArrayList<String>(
                EloraConfig.checkoutSwitchChildrenMap.keySet());

        // Filter the predicates if needed
        if (filteredPredicates != null && !filteredPredicates.isEmpty()) {
            switchablePredicates.retainAll(filteredPredicates);
        }

        List<Resource> predicates = new ArrayList<Resource>();
        for (String predicateUri : switchablePredicates) {
            Resource predicate = new ResourceImpl(predicateUri);
            predicates.add(predicate);
        }

        // Get switchable relation of base
        DocumentModel base = EloraDocumentHelper.getBaseVersion(subjectWcDoc);
        if (base != null) {

            List<Statement> switchableStmts = EloraRelationHelper.getStatements(
                    base, predicates);
            for (Statement stmt : switchableStmts) {

                String switchChildrenOption = EloraConfig.checkoutSwitchChildrenMap.get(
                        stmt.getPredicate().getUri());

                if (!switchChildrenOption.equals(
                        RelationSwitchHelper.CHECKOUT_SWITCH_CHILDREN_OPTION_WORKING_COPIES)) {

                    EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                            stmt);

                    DocumentModel objectRealDoc = RelationHelper.getDocumentModel(
                            stmtInfo.getObject(), session);

                    if (objectRealDoc == null) {
                        log.error(logInitMsg
                                + "Object is null. Relation is broken or unreadable. predicateUri = |"
                                + stmt.getPredicate().getUri()
                                + "|, subject docUid = |" + base.getId() + "|");
                        throw new EloraException(
                                "Object is null. Relation is broken or unreadable.");
                    }

                    DocumentModel objectWcDoc = session.getWorkingCopy(
                            objectRealDoc.getRef());
                    DocumentModel newObjectRealDoc = RelationSwitchHelper.getSwitchedObjectVersion(
                            objectRealDoc, objectWcDoc, switchChildrenOption);

                    edrm.updateRelation(session, subjectWcDoc,
                            stmtInfo.getPredicate().getUri(), objectWcDoc,
                            newObjectRealDoc, stmtInfo.getQuantity(),
                            stmtInfo.getOrdering(),
                            stmtInfo.getDirectorOrdering(),
                            stmtInfo.getViewerOrdering(),
                            stmtInfo.getInverseViewerOrdering(),
                            stmtInfo.getIsManual());

                    log.trace(logInitMsg
                            + "Switched relation's object for document |"
                            + subjectWcDoc.getId()
                            + "|, switch children option: |"
                            + switchChildrenOption + "|. Old object: |"
                            + objectWcDoc.getId() + "|. New object: |"
                            + objectRealDoc.getId() + "|.");
                }
            }
        }
    }

    private static DocumentModel getSwitchedObjectVersion(
            DocumentModel objectRealDoc, DocumentModel objectWcDoc,
            String switchChildrenOption) throws EloraException {

        switch (switchChildrenOption) {
        case CHECKOUT_SWITCH_CHILDREN_OPTION_AS_STORED:
            return objectRealDoc;
        case CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_RELEASED:
            DocumentModel switchedDoc = EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                    objectRealDoc);
            if (switchedDoc == null) {
                throw new EloraException("Document |" + objectRealDoc.getId()
                        + "| has no latest version or it is unreadable.");
            }
            return switchedDoc;
        case CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_VERSIONS:
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                    objectWcDoc);
            if (baseDoc != null) {
                baseDoc.refresh();
                return baseDoc;
            } else {
                return objectWcDoc;
            }
        default:
            throw new EloraException("Incorrect switch children option.");
        }
    }

}
