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

package com.aritu.eloraplm.core.relations.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

public class EloraRelationHelper {

    private static final Log log = LogFactory.getLog(EloraRelationHelper.class);

    private EloraRelationHelper() {
    }

    // TODO: Hacer funcion copyRelations(from, to) y sacar el origen y destino
    // antes. Hay que ver si serviria para todos los casos, como p.e. para
    // restablecer relaciones
    public static void copyRelationsToLastVersion(DocumentModel doc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        String logInitMsg = "[copyRelationsToLastVersion] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel subjectLastVersion = EloraDocumentHelper.getLatestVersion(
                doc, session);
        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, doc, null);

        for (Statement stmt : stmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);

            EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(stmt);

            if (object == null || object.isVersion() || !object.isVersionable()) {
                // Object is not a document or object is a document version or
                // object is not versionable
                eloraDocumentRelationManager.addRelation(session,
                        subjectLastVersion, stmt.getObject(),
                        stmt.getPredicate().getUri(),
                        eloraStmtInfo.getComment(),
                        eloraStmtInfo.getQuantity(),
                        eloraStmtInfo.getIsObjectWc(),
                        eloraStmtInfo.getOrdering());
            } else {
                DocumentModel objectLastVersion = EloraDocumentHelper.getLatestVersion(
                        object, session);
                if (objectLastVersion != null) {
                    // Create relation to last version
                    eloraDocumentRelationManager.addRelation(session,
                            subjectLastVersion, objectLastVersion,
                            stmt.getPredicate().getUri(),
                            eloraStmtInfo.getComment(),
                            eloraStmtInfo.getQuantity(),
                            eloraStmtInfo.getIsObjectWc(),
                            eloraStmtInfo.getOrdering());
                } else {
                    // If it is versionable and it has not any version throw
                    // error because it is not possible. We must control this
                    // from UI when we create a relation
                    throw new EloraException("Versionable object doc name=|"
                            + object.getName()
                            + "| without any version is not possible");
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static void restoreRelations(DocumentModel doc,
            VersionModel version,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        log.trace("Restoring relations of doc:|" + doc.getId() + "|");

        // Remove actual relations
        RelationHelper.removeRelation(doc, null, null);

        DocumentModel subjectVersion = session.getDocumentWithVersion(
                doc.getRef(), version);

        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, subjectVersion, null);

        for (Statement stmt : stmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            if (object.isProxy()) {
                throw new EloraException(
                        "Relation to a proxy document is not possible");
            }

            EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(stmt);

            if (object == null || (!object.isVersion() && !object.isProxy())
                    || !eloraStmtInfo.getIsObjectWc()) {
                // Object is not a document or object is a wc or object is a
                // document version and relation has to be restored to that
                // version
                eloraDocumentRelationManager.addRelation(session, doc,
                        stmt.getObject(), stmt.getPredicate().getUri(),
                        eloraStmtInfo.getComment(),
                        eloraStmtInfo.getQuantity(),
                        eloraStmtInfo.getIsObjectWc(),
                        eloraStmtInfo.getOrdering());
            } else if (eloraStmtInfo.getIsObjectWc()) {
                // Object is a document version and relation has to be restored
                // to object's wc
                DocumentModel objectWc = session.getSourceDocument(object.getRef());
                eloraDocumentRelationManager.addRelation(session, doc,
                        objectWc, stmt.getPredicate().getUri(),
                        eloraStmtInfo.getComment(),
                        eloraStmtInfo.getQuantity(),
                        eloraStmtInfo.getIsObjectWc(),
                        eloraStmtInfo.getOrdering());
            }
        }
    }

    /**
     * @param objectDoc
     * @param predicateResource
     * @return
     */
    public static List<Statement> getSubjectStatements(DocumentModel objectDoc,
            Resource predicateResource) {
        return getSubjectStatements(RelationConstants.GRAPH_NAME, objectDoc,
                predicateResource);
    }

    /**
     * @param graphName
     * @param objectDoc
     * @param predicateResource
     * @return
     */
    public static List<Statement> getSubjectStatements(String graphName,
            DocumentModel objectDoc, Resource predicateResource) {
        QNameResource objectDocResource = RelationHelper.getDocumentResource(objectDoc);
        return getRelationManager().getGraphByName(graphName).getStatements(
                null, predicateResource, objectDocResource);
    }

    /**
     * @return
     */
    public static RelationManager getRelationManager() {
        return Framework.getService(RelationManager.class);
    }

    // returns latest released document but related with object of stmt. If
    // there is not released doc then returns last related version
    public static DocumentModel getLatestRelatedReleasedVersion(
            DocumentModel subject, Statement stmt, CoreSession session)
            throws EloraException {

        DocumentModel latestReleased = null;

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);

        EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                new String[0]);

        String versionVersionableId = session.getWorkingCopy(subject.getRef()).getId();

        // TODO: Mirar si hay otra forma de sacar relaciones. Una consulta al
        // schema Relation...
        List<Statement> stmts = getRelationManager().getGraphByName(
                RelationConstants.GRAPH_NAME).getStatements(null,
                stmt.getPredicate(), stmt.getObject());
        List<String> uidList = new ArrayList<String>();
        for (Statement s : stmts) {
            uidList.add(RelationHelper.getDocumentModel(s.getSubject(), session).getId());
        }

        String query = EloraQueryFactory.getRelatedReleasedDoc(
                versionVersionableId, releasedStates, obsoleteStates,
                uidList.toArray(new String[0]));

        DocumentModelList relatedReleasedDocs = session.query(query);
        if (relatedReleasedDocs.size() > 0) {
            latestReleased = relatedReleasedDocs.get(0);
        } else {
            // Get latest related version
            query = EloraQueryFactory.getLatestRelatedDoc(versionVersionableId,
                    obsoleteStates, uidList.toArray(new String[0]));
            relatedReleasedDocs = session.query(query);
            if (relatedReleasedDocs.size() > 0) {
                latestReleased = relatedReleasedDocs.get(0);
            }
        }
        return latestReleased;
    }

    public static DocumentModel getLatestRelatedReleasedVersion(
            String versionVersionableId, List<String> uidList,
            CoreSession session) throws EloraException {

        DocumentModel latestReleased = null;

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);

        EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                new String[0]);

        String query = EloraQueryFactory.getRelatedReleasedDoc(
                versionVersionableId, releasedStates, obsoleteStates,
                uidList.toArray(new String[0]));

        DocumentModelList relatedReleasedDocs = session.query(query);
        if (relatedReleasedDocs.size() > 0) {
            latestReleased = relatedReleasedDocs.get(0);
        } else {
            // Get latest related version
            query = EloraQueryFactory.getLatestRelatedDoc(versionVersionableId,
                    obsoleteStates, uidList.toArray(new String[0]));
            relatedReleasedDocs = session.query(query);
            if (relatedReleasedDocs.size() > 0) {
                latestReleased = relatedReleasedDocs.get(0);
            }
        }
        return latestReleased;
    }

    public static DocumentModelList getSpecialRelatedReleased(
            DocumentRef subjectRef, Statement stmt, CoreSession session)
            throws EloraException {

        DocumentModelList relatedDocs = new DocumentModelListImpl();

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);

        EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                new String[0]);

        String versionVersionableId = session.getWorkingCopy(subjectRef).getId();

        List<Statement> stmts = getRelationManager().getGraphByName(
                RelationConstants.GRAPH_NAME).getStatements(null,
                stmt.getPredicate(), stmt.getObject());
        List<String> uidList = new ArrayList<String>();
        for (Statement s : stmts) {
            DocumentModel subjectDoc = RelationHelper.getDocumentModel(
                    s.getSubject(), session);
            // SE PUEDEN COMPARAR ESTOS???
            if (subjectDoc.getVersionSeriesId().equals(versionVersionableId)) {
                // It is a version of subject passed as parameter. It is not
                // another document pointing to stmt.getObject()
                uidList.add(subjectDoc.getId());
                relatedDocs.add(subjectDoc);
            }
        }

        String query = EloraQueryFactory.getRelatedReleasedDoc(
                versionVersionableId, releasedStates, obsoleteStates,
                uidList.toArray(new String[0]));

        DocumentModelList relatedReleasedDocs = session.query(query);
        if (relatedReleasedDocs.size() > 0) {
            // In this case we just want released and not all previously saved
            // in relatedDocs
            relatedDocs.clear();
            relatedDocs.addAll(relatedReleasedDocs);
        }
        return relatedDocs;
    }

}
