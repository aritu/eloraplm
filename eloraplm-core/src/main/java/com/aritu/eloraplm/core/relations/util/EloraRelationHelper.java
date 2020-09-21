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
import java.util.Collections;
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
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.EloraCoreGraph;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.api.ObjectList;
import com.aritu.eloraplm.core.relations.api.PredicateList;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
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
            CoreSession session)
            throws CheckinNotAllowedException, EloraException {

        String logInitMsg = "[copyRelationsToLastVersion] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Getting last version of document |"
                + doc.getId() + "|");
        DocumentModel subjectLastVersion = EloraDocumentHelper.getLatestVersion(
                doc);
        if (subjectLastVersion == null) {
            throw new EloraException("Document |" + doc.getId()
                    + "| has no latest version or it is unreadable.");
        }
        log.trace(logInitMsg + "Last version retrieved |"
                + subjectLastVersion.getId() + "|");
        log.trace(logInitMsg + "Getting statements of document |" + doc.getId()
                + "|");
        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, doc, null);
        log.trace(logInitMsg + "Number of statements retrieved: |"
                + stmts.size() + "|");

        List<String> relationsWithoutStateControl = getRelationsWithoutStateControl();
        for (Statement stmt : stmts) {
            if (EloraRelationConstants.HAS_ELORA_DRAFT_RELATION.equals(
                    stmt.getPredicate().getUri())) {
                continue;
            }

            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            log.trace(logInitMsg + "Relation object |" + object.getId()
                    + "| retrieved");
            // TODO: Ponemos esto para no controlar las relaciones de este tipo
            // en el checkin. En un futuro hay que ver como pasar esto a la
            // configuracion. Ahora se decide poner el control aquí porque es lo
            // más rápido y porque en el promote de los BOM tampoco se tienen en
            // cuenta estas relaciones para poner OK/KO
            if (!relationsWithoutStateControl.contains(
                    stmt.getPredicate().getUri())) {
                if (!object.isCheckedOut() && LifecyclesConfig.isSupported(
                        doc.getCurrentLifeCycleState(),
                        object.getCurrentLifeCycleState())) {
                    copyRelation(doc, object, eloraDocumentRelationManager,
                            subjectLastVersion, stmt);
                } else {
                    throw new CheckinNotAllowedException(doc, object);
                }
            } else {
                copyRelation(doc, object, eloraDocumentRelationManager,
                        subjectLastVersion, stmt);
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static List<String> getRelationsWithoutStateControl() {
        List<String> noStateControlRelationList = new ArrayList<String>();
        noStateControlRelationList.addAll(
                RelationsConfig.bomAnarchicRelationsList);
        noStateControlRelationList.add(EloraRelationConstants.BOM_HAS_DOCUMENT);
        noStateControlRelationList.add(EloraRelationConstants.CAD_HAS_DOCUMENT);

        return noStateControlRelationList;
    }

    public static void copyAllRelationsButAnarchicsToVersion(DocumentModel from,
            DocumentModel to,
            EloraDocumentRelationManager eloraDocumentRelationManager)
            throws CheckinNotAllowedException, EloraException {

        CoreSession session = from.getCoreSession();
        String logInitMsg = "[copyAllRelationsButAnarchicsToVersion] ["
                + session.getPrincipal().getName() + "] ";

        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, from, null);
        for (Statement stmt : stmts) {
            if (!RelationsConfig.bomAnarchicRelationsList.contains(
                    stmt.getPredicate().getUri())) {
                DocumentModel object = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);
                if (object == null) {
                    log.error(logInitMsg
                            + "Object is null. Relation is broken or unreadable. predicateUri = |"
                            + stmt.getPredicate().getUri()
                            + "|, subject docId = |" + from.getId() + "|");
                    throw new EloraException(
                            "Object is null. Relation is broken or unreadable.");
                }

                if (!object.isCheckedOut() && LifecyclesConfig.isSupported(
                        to.getCurrentLifeCycleState(),
                        object.getCurrentLifeCycleState())) {
                    copyRelation(from, object, eloraDocumentRelationManager, to,
                            stmt);
                } else {
                    throw new CheckinNotAllowedException(from, object);
                }
            }
        }
    }

    private static void copyRelation(DocumentModel subject,
            DocumentModel object,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            DocumentModel subjectLastVersion, Statement stmt)
            throws EloraException {
        CoreSession session = subject.getCoreSession();
        String logInitMsg = "[copyRelation] ["
                + session.getPrincipal().getName() + "] ";

        EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(stmt);
        if (isOtherDocument(object)) {
            log.trace(logInitMsg + "Object |" + object.getId()
                    + "| is other document");
            eloraDocumentRelationManager.addRelation(session,
                    subjectLastVersion, stmt.getObject(),
                    stmt.getPredicate().getUri());
            log.trace(logInitMsg + "Relation added from |"
                    + subjectLastVersion.getId() + "| to |"
                    + stmt.getObject().toString() + "| with predicate |"
                    + stmt.getPredicate().getUri() + "|");
        } else if (isVersionOrNotVersionableDocument(object)) {
            log.trace(logInitMsg + "Object |" + object.getId()
                    + "| is a version or not versionable");
            eloraDocumentRelationManager.addRelation(session,
                    subjectLastVersion, stmt.getObject(),
                    stmt.getPredicate().getUri(), eloraStmtInfo.getComment(),
                    eloraStmtInfo.getQuantity(), eloraStmtInfo.getOrdering(),
                    eloraStmtInfo.getDirectorOrdering(),
                    eloraStmtInfo.getViewerOrdering(),
                    eloraStmtInfo.getInverseViewerOrdering(),
                    eloraStmtInfo.getIsManual());
            log.trace(logInitMsg + "Relation added from |"
                    + subjectLastVersion.getId() + "| to |"
                    + stmt.getObject().toString() + "| with predicate |"
                    + stmt.getPredicate().getUri() + "|");
            if (object.isVersion()) {
                restoreWorkingCopyRelation(subject, object,
                        stmt.getPredicate().getUri(), eloraStmtInfo,
                        eloraDocumentRelationManager, session);
            }
        } else {
            log.trace(logInitMsg + "Object |" + object.getId()
                    + "| is a versionable working copy");
            DocumentModel objectLastVersion = EloraDocumentHelper.getBaseVersion(
                    object);
            addDocumentRelation(subjectLastVersion, objectLastVersion, object,
                    eloraDocumentRelationManager, session, stmt, eloraStmtInfo);
            removeAnarchicRelations(subject, stmt, object, objectLastVersion,
                    eloraDocumentRelationManager, session);
        }
    }

    private static boolean isOtherDocument(DocumentModel object) {
        return object == null;
    }

    private static boolean isVersionOrNotVersionableDocument(
            DocumentModel object) {
        return object.isVersion() || !object.isVersionable();
    }

    private static void restoreWorkingCopyRelation(DocumentModel wcDoc,
            DocumentModel versionObject, String predicate,
            EloraStatementInfo eloraStmtInfo,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        // RelationHelper.removeRelation(wcDoc, eloraStmtInfo.getPredicate(),
        // versionObject);
        eloraDocumentRelationManager.softDeleteRelation(session, wcDoc,
                predicate, versionObject);

        DocumentModel wcObject = session.getWorkingCopy(versionObject.getRef());
        eloraDocumentRelationManager.addRelation(session, wcDoc, wcObject,
                predicate, eloraStmtInfo.getComment(),
                eloraStmtInfo.getQuantity(), eloraStmtInfo.getOrdering(),
                eloraStmtInfo.getDirectorOrdering(),
                eloraStmtInfo.getViewerOrdering(),
                eloraStmtInfo.getInverseViewerOrdering(),
                eloraStmtInfo.getIsManual());
    }

    private static void addDocumentRelation(DocumentModel subjectLastVersion,
            DocumentModel objectLastVersion, DocumentModel object,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session, Statement stmt,
            EloraStatementInfo eloraStmtInfo) throws EloraException {

        String logInitMsg = "[addDocumentRelation] ["
                + session.getPrincipal().getName() + "] ";
        if (objectLastVersion != null) {
            eloraDocumentRelationManager.addRelation(session,
                    subjectLastVersion, objectLastVersion,
                    stmt.getPredicate().getUri(), eloraStmtInfo.getComment(),
                    eloraStmtInfo.getQuantity(), eloraStmtInfo.getOrdering(),
                    eloraStmtInfo.getDirectorOrdering(),
                    eloraStmtInfo.getViewerOrdering(),
                    eloraStmtInfo.getInverseViewerOrdering(),
                    eloraStmtInfo.getIsManual());
            log.trace(logInitMsg + "Relation added from |"
                    + subjectLastVersion.getId() + "| to |"
                    + objectLastVersion.getId() + "| with predicate |"
                    + stmt.getPredicate().getUri() + "|");

        } else {
            throw new EloraException(
                    "Versionable object doc name=|" + object.getTitle()
                            + "| without any version is not possible");
        }
    }

    private static void removeAnarchicRelations(DocumentModel doc,
            Statement stmt, DocumentModel object,
            DocumentModel objectLastVersion,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {
        String logInitMsg = "[removeAnarchicRelations]";

        if (RelationsConfig.bomAnarchicRelationsList.contains(
                stmt.getPredicate().getUri())) {
            if (!LifecyclesConfig.releasedStatesList.contains(
                    object.getCurrentLifeCycleState())) {
                List<VersionModel> versionModels = doc.getCoreSession().getVersionsForDocument(
                        doc.getRef());
                if (versionModels.size() > 1) {
                    VersionModel previousVersion = versionModels.get(
                            versionModels.size() - 2);
                    DocumentModel previousVersionDoc = doc.getCoreSession().getDocumentWithVersion(
                            doc.getRef(), previousVersion);
                    // RelationHelper.removeRelation(previousVersionDoc,
                    // stmt.getPredicate(), objectLastVersion);
                    eloraDocumentRelationManager.softDeleteRelation(session,
                            previousVersionDoc, stmt.getPredicate().getUri(),
                            objectLastVersion);
                    log.trace(logInitMsg + "Anarchic relation removed from |"
                            + previousVersionDoc.getId() + "| to |"
                            + objectLastVersion.getId() + "| with predicate |"
                            + stmt.getPredicate().getUri() + "|");
                }
            }
        }
    }

    public static void copyIncomingAnarchicRelationsToLastVersion(
            DocumentModel doc,
            EloraDocumentRelationManager eloraDocumentRelationManager)
            throws EloraException {

        CoreSession session = doc.getCoreSession();
        String logInitMsg = "[copyIncomingAnarchicRelationsToLastVersion] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO: Mirar la forma de chequear si el objeto es 'nuevo'
        DocumentModel objectBaseDoc = EloraDocumentHelper.getLatestVersion(doc);
        if (objectBaseDoc == null) {
            throw new EloraException("Document |" + doc.getId()
                    + "| has no latest version or it is unreadable.");
        }
        log.trace(logInitMsg + "Base document |" + objectBaseDoc.getId()
                + "| retrieved from document |" + doc.getId() + "|");

        // TODO: Sacar de configuracion!!!
        String[] anarchicPredicateList = { EloraRelationConstants.BOM_HAS_BOM,
                EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT,
                EloraRelationConstants.BOM_MANUFACTURER_HAS_PART };
        for (String predicateUri : anarchicPredicateList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, doc,
                    predicateResource);
            log.trace("Retrieved |" + stmts.size()
                    + "| statements of predicate |" + predicateUri + "|");
            for (Statement stmt : stmts) {
                DocumentModel subject = RelationHelper.getDocumentModel(
                        stmt.getSubject(), session);
                if (subject == null) {
                    log.error(logInitMsg
                            + "Subject is null. Relation is broken or unreadable. predicateUri = |"
                            + predicateUri + "|, object docId = |" + doc.getId()
                            + "|");
                    throw new EloraException(
                            "Subject is null. Relation is broken or unreadable.");
                }

                EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(
                        stmt);
                DocumentModel subjectBaseDoc = EloraDocumentHelper.getLatestVersion(
                        subject);
                if (subjectBaseDoc != null) {
                    // Check just if subject has a version. Object has been
                    // checked in so we don't have to check it.
                    // Create relation to latest versions
                    eloraDocumentRelationManager.addRelation(session,
                            subjectBaseDoc, objectBaseDoc,
                            stmt.getPredicate().getUri(),
                            eloraStmtInfo.getComment(),
                            eloraStmtInfo.getQuantity(),
                            eloraStmtInfo.getOrdering(),
                            eloraStmtInfo.getDirectorOrdering(),
                            eloraStmtInfo.getViewerOrdering(),
                            eloraStmtInfo.getInverseViewerOrdering(),
                            eloraStmtInfo.getIsManual());
                    log.trace(logInitMsg + "Relation added from |"
                            + subjectBaseDoc.getId() + "| to |"
                            + objectBaseDoc.getId() + "| with predicate |"
                            + stmt.getPredicate().getUri() + "|");
                } else {
                    log.warn(logInitMsg + "Subject |" + subject.getTitle()
                            + "| without any version in anarchic relation");
                    // throw new EloraException("Versionable subject doc name=|"
                    // + subject.getName()
                    // + "| without any version in anarchic relation");
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static void restoreRelations(DocumentModel doc, VersionModel version,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {
        String logInitMsg = "[restoreRelations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Restoring relations of doc:|" + doc.getId()
                + "|");

        if (!EloraDocumentHelper.isWorkingCopy(doc)) {
            doc = session.getWorkingCopy(doc.getRef());
        }

        // DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
        DocumentModel subjectVersion = session.getDocumentWithVersion(
                doc.getRef(), version);
        // if (!baseDoc.getId().equals(subjectVersion.getId())) {
        restoreAnarchicRelations(subjectVersion, eloraDocumentRelationManager,
                session);
        // }

        // Remove actual relations
        log.trace(logInitMsg + "About to remove actual relations of doc |"
                + doc.getId() + "|");
        // RelationHelper.removeRelation(doc, null, null);
        eloraDocumentRelationManager.softDeleteRelation(session, doc, null,
                null);
        log.trace(logInitMsg + "All relations removed from doc |" + doc.getId()
                + "|");

        // TODO: Hay otra funcion copyRelations que habria que juntar con esta
        copyAllRelations(subjectVersion, doc, eloraDocumentRelationManager,
                session);
    }

    private static void restoreAnarchicRelations(DocumentModel doc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {
        String logInitMsg = "[restoreAnarchicRelations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Restore |" + doc.getId()
                + "| version's anarchic relations");
        // TODO: Puede sacar
        List<Statement> stmts = getAnarchicStatements(doc);
        List<String> processedObjList = new ArrayList<String>();
        for (Statement stmt : stmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            if (!processedObjList.contains(object.getVersionSeriesId())) {
                DocumentModel latestObject = session.getLastDocumentVersion(
                        object.getRef());
                if (!latestObject.getId().equals(object.getId())) {
                    copyRelationsToTheRestOfObjectVersions(doc, object, stmt,
                            eloraDocumentRelationManager, session);
                }
                processedObjList.add(object.getVersionSeriesId());
            }
        }
        log.trace(logInitMsg + "Finished restoring |" + doc.getId()
                + "| version's anarchic relations");
    }

    private static void copyRelationsToTheRestOfObjectVersions(
            DocumentModel doc, DocumentModel object, Statement stmt,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        String logInitMsg = "[copyRelationsToTheRestOfObjectVersions] ["
                + session.getPrincipal().getName() + "] ";

        List<DocumentModel> objectVersionList = session.getVersions(
                object.getRef());
        List<String> idList = new ArrayList<String>();
        for (DocumentModel objectVersion : objectVersionList) {
            idList.add(objectVersion.getId());
        }
        int firstObjIndex = idList.indexOf(object.getId());
        if (firstObjIndex < idList.size() - 1) {
            for (int i = firstObjIndex + 1; i < objectVersionList.size(); i++) {
                EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(
                        stmt);
                try {
                    eloraDocumentRelationManager.addRelation(session, doc,
                            objectVersionList.get(i),
                            stmt.getPredicate().getUri(),
                            eloraStmtInfo.getComment(),
                            eloraStmtInfo.getQuantity(),
                            eloraStmtInfo.getOrdering(),
                            eloraStmtInfo.getDirectorOrdering(),
                            eloraStmtInfo.getViewerOrdering(),
                            eloraStmtInfo.getInverseViewerOrdering(),
                            eloraStmtInfo.getIsManual());
                } catch (RelationAlreadyExistsException e) {
                    log.trace(logInitMsg
                            + "Anarchic relation already exists but is ignored");
                }
            }
        }
    }

    public static List<Statement> getAnarchicStatements(DocumentModel from) {
        List<Resource> anarchicPredicates = new ArrayList<Resource>();
        for (String predicateUri : RelationsConfig.bomAnarchicRelationsList) {
            Resource predicate = new ResourceImpl(predicateUri);
            anarchicPredicates.add(predicate);
        }
        List<Statement> stmts = EloraRelationHelper.getStatements(from,
                anarchicPredicates);
        return stmts;
    }

    public static void checkForNoAnarchicParents(DocumentModel doc)
            throws EloraException {

        List<Statement> stmts = getSubjectStatements(doc, null);
        for (Statement stmt : stmts) {
            if (!RelationsConfig.bomAnarchicRelationsList.contains(
                    stmt.getPredicate().getUri())) {
                throw new EloraException("Document |" + doc.getId()
                        + "| has no anarchic relations as parent");
            }
        }
    }

    private static void copyAllRelations(DocumentModel from, DocumentModel to,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        String logInitMsg = "[copyAllRelations] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Copy all relations from:|" + from.getId()
                + "| to |" + to.getId() + "|");

        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, from, null);

        for (Statement stmt : stmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            if (object == null) {
                log.error(logInitMsg
                        + "Object is null. Relation is broken or unreadable. predicateUri = |"
                        + stmt.getPredicate().getUri() + "|, subject docId = |"
                        + from.getId() + "|");
                throw new EloraException(
                        "Object is null. Relation is broken or unreadable.");
            }

            if (object.isProxy()) {
                throw new EloraException(
                        "Relation to a proxy document is not possible");
            }

            EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(stmt);

            if (isOtherDocument(object)) {
                eloraDocumentRelationManager.addRelation(session, to,
                        stmt.getObject(), stmt.getPredicate().getUri());
            } else if (!object.isVersionable()) {
                eloraDocumentRelationManager.addRelation(session, to,
                        stmt.getObject(), stmt.getPredicate().getUri(),
                        eloraStmtInfo.getComment(), eloraStmtInfo.getQuantity(),
                        eloraStmtInfo.getOrdering(),
                        eloraStmtInfo.getDirectorOrdering(),
                        eloraStmtInfo.getViewerOrdering(),
                        eloraStmtInfo.getInverseViewerOrdering(),
                        eloraStmtInfo.getIsManual());
            } else {
                DocumentModel objectWc = session.getSourceDocument(
                        object.getRef());

                if (RelationsConfig.bomAnarchicRelationsList.contains(
                        stmt.getPredicate().getUri())) {
                    try {
                        eloraDocumentRelationManager.addRelation(session, to,
                                objectWc, stmt.getPredicate().getUri(),
                                eloraStmtInfo.getComment(),
                                eloraStmtInfo.getQuantity(),
                                eloraStmtInfo.getOrdering(),
                                eloraStmtInfo.getDirectorOrdering(),
                                eloraStmtInfo.getViewerOrdering(),
                                eloraStmtInfo.getInverseViewerOrdering(),
                                eloraStmtInfo.getIsManual());
                    } catch (RelationAlreadyExistsException e) {
                        log.trace(logInitMsg
                                + "Anarchic relation already exists but is ignored");
                    }
                } else {
                    eloraDocumentRelationManager.addRelation(session, to,
                            objectWc, stmt.getPredicate().getUri(),
                            eloraStmtInfo.getComment(),
                            eloraStmtInfo.getQuantity(),
                            eloraStmtInfo.getOrdering(),
                            eloraStmtInfo.getDirectorOrdering(),
                            eloraStmtInfo.getViewerOrdering(),
                            eloraStmtInfo.getInverseViewerOrdering(),
                            eloraStmtInfo.getIsManual());
                }
            }
        }
        log.trace(logInitMsg + "Relations copied from:|" + from.getId()
                + "| to |" + to.getId() + "|");
    }

    public static List<Statement> getStatements(DocumentModel subjectDoc,
            List<Resource> predicates) {

        if (predicates.isEmpty()) {
            return Collections.emptyList();
        }

        QNameResource docResource = RelationHelper.getDocumentResource(
                subjectDoc);
        EloraCoreGraph eg = (EloraCoreGraph) getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);
        return eg.getStatements(docResource, new PredicateList(predicates),
                null);
    }

    public static List<Statement> getStatements(DocumentModel subjectDoc,
            Resource predicate, List<DocumentModel> objectDocs) {

        if (objectDocs.isEmpty()) {
            return Collections.emptyList();
        }

        QNameResource docResource = RelationHelper.getDocumentResource(
                subjectDoc);

        List<QNameResource> objectResources = convertDocListToResources(
                objectDocs);

        EloraCoreGraph eg = (EloraCoreGraph) getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);
        return eg.getStatements(docResource, predicate,
                new ObjectList(objectResources));
    }

    private static List<QNameResource> convertDocListToResources(
            List<DocumentModel> objectDocs) {
        List<QNameResource> list = new ArrayList<>();
        for (DocumentModel object : objectDocs) {
            list.add(RelationHelper.getDocumentResource(object));
        }
        return list;
    }

    public static Statement getStatement(String graphName,
            DocumentModel subjectDoc, Resource predicate,
            DocumentModel objectDoc) {
        QNameResource subjectResource = RelationHelper.getDocumentResource(
                subjectDoc);
        QNameResource objectResource = RelationHelper.getDocumentResource(
                objectDoc);
        List<Statement> stmts = getRelationManager().getGraphByName(
                graphName).getStatements(subjectResource, predicate,
                        objectResource);

        if (stmts.size() > 0) {
            return stmts.get(0);
        } else {
            return null;
        }
    }

    public static List<Statement> getSubjectStatementsByPredicateList(
            DocumentModel objectDoc, List<Resource> predicates) {

        if (predicates.isEmpty()) {
            return Collections.emptyList();
        }

        QNameResource docResource = RelationHelper.getDocumentResource(
                objectDoc);
        EloraCoreGraph eg = (EloraCoreGraph) getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);
        return eg.getStatements(null, new PredicateList(predicates),
                docResource);
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
        QNameResource objectDocResource = RelationHelper.getDocumentResource(
                objectDoc);
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
    @Deprecated
    public static DocumentModel getLatestRelatedReleasedVersion(
            DocumentModel subject, Statement stmt, CoreSession session)
            throws EloraException {

        DocumentModel latestReleased = null;

        String versionVersionableId = subject.getVersionSeriesId();

        // TODO: Mirar si hay otra forma de sacar relaciones. Una consulta al
        // schema Relation...
        List<Statement> stmts = getRelationManager().getGraphByName(
                RelationConstants.GRAPH_NAME).getStatements(null,
                        stmt.getPredicate(), stmt.getObject());
        List<String> uidList = new ArrayList<>();
        for (Statement s : stmts) {
            uidList.add(RelationHelper.getDocumentModel(s.getSubject(),
                    session).getId());
        }

        String query = EloraQueryFactory.getRelatedReleasedDocQuery(
                subject.getType(), versionVersionableId, uidList);

        DocumentModelList relatedReleasedDocs = session.query(query);
        if (relatedReleasedDocs.size() > 0) {
            latestReleased = relatedReleasedDocs.get(0);
        } else {
            // Get latest related version
            query = EloraQueryFactory.getLatestRelatedDocQuery(
                    subject.getType(), versionVersionableId, uidList, true);
            relatedReleasedDocs = session.query(query);
            if (relatedReleasedDocs.size() > 0) {
                latestReleased = relatedReleasedDocs.get(0);
            }
        }
        return latestReleased;
    }

    public static DocumentModel getLatestRelatedVersion(CoreSession session,
            Long majorVersion, List<String> uidList, String type) {

        DocumentModel latestDoc = getLatestReleasedOrObsoleteInMajorVersion(
                session, majorVersion, uidList, type);
        if (latestDoc == null) {
            latestDoc = EloraQueryFactory.getLatestInMajorVersion(session,
                    majorVersion, uidList, type);
        }
        return latestDoc;
    }

    private static DocumentModel getLatestReleasedOrObsoleteInMajorVersion(
            CoreSession session, Long majorVersion, List<String> uidList,
            String type) {

        List<String> stateList = new ArrayList<>();
        stateList.addAll(LifecyclesConfig.releasedStatesList);
        stateList.addAll(LifecyclesConfig.obsoleteStatesList);

        return EloraQueryFactory.getLatestByStatesInMajorVersion(session,
                majorVersion, stateList, uidList, type);
    }

    public static DocumentModelList getSpecialRelatedReleased(
            DocumentRef subjectRef, Statement stmt, CoreSession session)
            throws EloraException {

        String logInitMsg = "[getSpecialRelatedReleased] ["
                + session.getPrincipal().getName() + "] ";

        DocumentModelList relatedDocs = new DocumentModelListImpl();

        DocumentModel wcDoc = session.getWorkingCopy(subjectRef);
        if (wcDoc == null) {
            log.error(logInitMsg + "Working copy of |" + subjectRef.toString()
                    + "| is null. It does not exist or is unreadable.");
            throw new EloraException(
                    "Document does not exist or is unreadable.");
        }
        String versionVersionableId = wcDoc.getId();

        List<Statement> stmts = getRelationManager().getGraphByName(
                RelationConstants.GRAPH_NAME).getStatements(null,
                        stmt.getPredicate(), stmt.getObject());
        List<String> uidList = new ArrayList<>();
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

        String query = EloraQueryFactory.getRelatedReleasedDocQuery(
                wcDoc.getType(), versionVersionableId, uidList);

        DocumentModelList relatedReleasedDocs = session.query(query);
        if (relatedReleasedDocs.size() > 0) {
            // In this case we just want released and not all previously saved
            // in relatedDocs
            relatedDocs.clear();
            relatedDocs.addAll(relatedReleasedDocs);
        }
        return relatedDocs;
    }

    public static boolean existsRelation(DocumentModel subject,
            DocumentModel object, String predicateUri, CoreSession session) {

        EloraCoreGraph eg = (EloraCoreGraph) getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);
        List<Statement> stmts = eg.getStatements(
                RelationHelper.getDocumentResource(subject),
                new ResourceImpl(predicateUri),
                RelationHelper.getDocumentResource(object));

        if (stmts.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static DocumentModelList getAllVersionsOfRelatedObject(
            DocumentModel subjectDoc, DocumentModel objectDoc,
            String predicateUri, CoreSession session) {

        List<DocumentRef> objectVersionRefs = session.getVersionsRefs(
                objectDoc.getRef());
        objectVersionRefs.add(
                session.getWorkingCopy(objectDoc.getRef()).getRef());

        DocumentModelList relatedVersionsOfObject = new DocumentModelListImpl();

        List<DocumentModel> objectDocs = new ArrayList<>();
        for (DocumentRef docRef : objectVersionRefs) {
            objectDocs.add(session.getDocument(docRef));
        }

        List<Statement> stmts = getStatements(subjectDoc,
                new ResourceImpl(predicateUri), objectDocs);
        for (Statement stmt : stmts) {
            relatedVersionsOfObject.add(
                    RelationHelper.getDocumentModel(stmt.getObject(), session));
        }

        return relatedVersionsOfObject;
    }

    public static boolean isCircularRelation(DocumentModel parentDoc,
            DocumentModel addedDoc, CoreSession session) {
        try {
            checkCircularRelation(parentDoc, addedDoc, session);
            return false;
        } catch (EloraException e) {
            return true;
        }
    }

    // TODO: Que sea mas eficiente sacando las relaciones de una lista de
    // predicates. Hay que saber cuales queremos
    private static void checkCircularRelation(DocumentModel parentDoc,
            DocumentModel addedDoc, CoreSession session) throws EloraException {

        if (parentDoc.getId().equals(addedDoc.getId())) {
            throw new EloraException("Circular relation adding document");
        }

        List<Statement> stmts = RelationHelper.getStatements(addedDoc, null);
        for (Statement stmt : stmts) {
            DocumentModel relatedDoc = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            if (relatedDoc.getId().equals(parentDoc.getId())) {
                throw new EloraException("Circular relation adding document");
            }
            checkCircularRelation(parentDoc, relatedDoc, session);
        }
    }

}
