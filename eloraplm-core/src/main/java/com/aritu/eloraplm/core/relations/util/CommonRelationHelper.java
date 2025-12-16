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
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.CommonRelationConstants;
import com.aritu.eloraplm.core.relations.CoreRelationGraph;
import com.aritu.eloraplm.core.relations.RelationGraph;
import com.aritu.eloraplm.core.relations.api.ObjectList;
import com.aritu.eloraplm.core.relations.api.PredicateList;
import com.aritu.eloraplm.core.relations.api.RelationManagementService;

public class CommonRelationHelper {

    // TODO BEHARREZKOA BADA ALDATU LEIKE, ORAIN ARTE FALSE BIDALI DA
    // protected static final boolean INCLUDE_STATEMENT_IN_EVENTS = false;

    /*
     * ==================================================================
     * SERVICES
     * ==================================================================
     */

    public static CoreRelationGraph getCoreGraph() {
        return (CoreRelationGraph) getRelationManager().getGraphByName(
                CommonRelationConstants.GRAPH_CORE_RELATION);
    }

    protected static RelationManager getRelationManager() {
        return Framework.getService(RelationManager.class);
    }

    protected static RelationManagementService getRms() {
        return Framework.getService(RelationManagementService.class);
    }

    /*
     * ==================================================================
     * QUERIES
     * ==================================================================
     */

    /**
     * @param subjectDoc
     * @param predicate
     * @return
     */
    protected static List<Statement> getStatements(RelationGraph graph,
            DocumentModel subjectDoc, Resource predicate) {
        List<Resource> predicates = null;
        if (predicate != null) {
            predicates = new ArrayList<Resource>();
            predicates.add(predicate);
        }
        return getStatementsByPredicateList(graph, subjectDoc, predicates);
    }

    /**
     * @param subjectDoc
     * @param predicates
     * @return
     */
    protected static List<Statement> getStatementsByPredicateList(
            RelationGraph graph, DocumentModel subjectDoc,
            List<Resource> predicates) {

        return getStatementsByObjectAndPredicateList(graph, subjectDoc,
                predicates, null);
    }

    /**
     * @param objectDoc
     * @param predicate
     * @return
     */
    protected static List<Statement> getSubjectStatements(RelationGraph graph,
            DocumentModel objectDoc, Resource predicate) {

        List<DocumentModel> objectDocs = null;
        if (objectDoc != null) {
            objectDocs = new ArrayList<DocumentModel>();
            objectDocs.add(objectDoc);
        }

        List<Resource> predicates = null;
        if (predicate != null) {
            predicates = new ArrayList<Resource>();
            predicates.add(predicate);
        }

        return getSubjectStatementsByObjectAndPredicateList(graph, objectDocs,
                predicates);
    }

    /**
     * @param objectDoc
     * @param predicates
     * @return
     */
    protected static List<Statement> getSubjectStatementsByPredicateList(
            RelationGraph graph, DocumentModel objectDoc,
            List<Resource> predicates) {

        List<DocumentModel> objectDocs = null;
        if (objectDoc != null) {
            objectDocs = new ArrayList<DocumentModel>();
            objectDocs.add(objectDoc);
        }

        return getStatementsByObjectAndPredicateList(graph, null, predicates,
                objectDocs);
    }

    /**
     * @param objectDocs
     * @param predicates
     * @return
     */
    protected static List<Statement> getSubjectStatementsByObjectAndPredicateList(
            RelationGraph graph, List<DocumentModel> objectDocs,
            List<Resource> predicates) {

        return getStatementsByObjectAndPredicateList(graph, null, predicates,
                objectDocs);
    }

    /**
     * @param subjectDoc
     * @param predicate
     * @param objectDocs
     * @return
     */
    protected static List<Statement> getStatements(RelationGraph graph,
            DocumentModel subjectDoc, Resource predicate,
            List<DocumentModel> objectDocs) {

        List<Resource> predicates = null;
        if (predicate != null) {
            predicates = new ArrayList<Resource>();
            predicates.add(predicate);
        }

        return getStatementsByObjectAndPredicateList(graph, subjectDoc,
                predicates, objectDocs);
    }

    /**
     * @param subjectDoc
     * @param predicates
     * @param objectDocs
     * @return
     */
    protected static List<Statement> getStatementsByObjectAndPredicateList(
            RelationGraph graph, DocumentModel subjectDoc,
            List<Resource> predicates, List<DocumentModel> objectDocs) {

        PredicateList predicatesRes = null;
        if (predicates != null) {
            if (predicates.isEmpty()) {
                return Collections.emptyList();
            }

            predicatesRes = new PredicateList(predicates);
        }

        QNameResource subjectRes = RelationHelper.getDocumentResource(
                subjectDoc);

        Node objects = null;
        if (objectDocs != null) {
            List<QNameResource> objectsRes = convertDocListToResources(
                    objectDocs);
            objects = new ObjectList(objectsRes);
        }

        return graph.getStatements(subjectRes, predicatesRes, objects);
    }

    /* CoreRelations */

    public static List<Statement> getCoreStatements(DocumentModel subjectDoc,
            Resource predicate) {
        return getStatements(getCoreGraph(), subjectDoc, predicate);
    }

    public static List<Statement> getCoreStatementsByPredicateList(
            DocumentModel subjectDoc, List<Resource> predicates) {

        return getStatementsByObjectAndPredicateList(getCoreGraph(), subjectDoc,
                predicates, null);
    }

    public static List<Statement> getCoreSubjectStatementsByPredicateList(
            DocumentModel objectDoc, List<Resource> predicates) {

        return getSubjectStatementsByPredicateList(getCoreGraph(), objectDoc,
                predicates);
    }

    public static List<Statement> getCoreSubjectStatementsByPredicateList(
            List<DocumentModel> objectDocs, List<Resource> predicates) {
        return getSubjectStatementsByObjectAndPredicateList(getCoreGraph(),
                objectDocs, predicates);
    }

    /*
     * ==================================================================
     * CREATE RELATIONS
     * ==================================================================
     */

    // Defined in helpers of each relation type

    /*
     * ==================================================================
     * UPDATE RELATIONS
     * ==================================================================
     */

    // Defined in helpers of each relation type

    /*
     * ==================================================================
     * REMOVE RELATIONS
     * ==================================================================
     */

    public static void removeRelation(RelationGraph graph, CoreSession session,
            Statement stmt) {

        getRms().softDeleteRelation(session, graph, stmt);
    }

    public static void removeRelation(RelationGraph graph, CoreSession session,
            DocumentModel from, String predicate, DocumentModel to) {

        getRms().softDeleteRelation(session, graph, from, predicate, to);
    }

    /*
     * ==================================================================
     * UTIL FUNCTIONS
     * ==================================================================
     */

    public static DocumentModelList getObjectDocuments(RelationGraph graph,
            DocumentModel subjectDoc, Resource predicate) {
        List<Statement> stmts = getStatements(graph, subjectDoc, predicate);
        if (stmts != null) {
            DocumentModelList docs = new DocumentModelListImpl();
            for (Statement stmt : stmts) {
                DocumentModel d = getDocumentModel(stmt.getObject(),
                        subjectDoc.getCoreSession());
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
        return null;
    }

    public static DocumentModel getDocumentModel(Node node,
            CoreSession session) {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.<String, Object> singletonMap(
                    ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
            Object o = getRelationManager().getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    public static DocumentModelList getAllRelatedVersionsOfObject(
            DocumentModel subjectDoc, DocumentModel objectDoc,
            String predicateUri, CoreSession session) {

        // Add AVs
        List<DocumentRef> objectVersionRefs = session.getVersionsRefs(
                objectDoc.getRef());
        // Add WC
        objectVersionRefs.add(
                session.getWorkingCopy(objectDoc.getRef()).getRef());

        DocumentModelList relatedVersionsOfObject = new DocumentModelListImpl();

        List<DocumentModel> objectDocs = new ArrayList<>();
        for (DocumentRef docRef : objectVersionRefs) {
            objectDocs.add(session.getDocument(docRef));
        }

        List<Statement> stmts = getStatements(getCoreGraph(), subjectDoc,
                new ResourceImpl(predicateUri), objectDocs);
        for (Statement stmt : stmts) {
            relatedVersionsOfObject.add(
                    RelationHelper.getDocumentModel(stmt.getObject(), session));
        }

        return relatedVersionsOfObject;
    }

    private static List<QNameResource> convertDocListToResources(
            List<DocumentModel> docs) {
        List<QNameResource> list = new ArrayList<>();
        for (DocumentModel doc : docs) {
            list.add(RelationHelper.getDocumentResource(doc));
        }
        return list;
    }

    public static boolean existsRelation(RelationGraph graph,
            DocumentModel subject, DocumentModel object, String predicateUri,
            CoreSession session) {

        List<Statement> stmts = graph.getStatements(
                RelationHelper.getDocumentResource(subject),
                new ResourceImpl(predicateUri),
                RelationHelper.getDocumentResource(object));

        if (stmts.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
