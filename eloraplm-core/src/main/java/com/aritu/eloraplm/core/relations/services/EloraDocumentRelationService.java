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
package com.aritu.eloraplm.core.relations.services;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.services.DocumentRelationService;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.EloraCoreGraph;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraDocumentRelationService extends DocumentRelationService
        implements EloraDocumentRelationManager {

    private static final Log log = LogFactory.getLog(
            EloraDocumentRelationService.class);

    // TODO: Los campos personales se pasan como string porque por ahora no
    // sabemos gestionarlos si pasamos integer, boolean etc. Esto es debido a la
    // clase Literal de nuxeo

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment,
            String quantity) {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, null, null, null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment,
            String quantity) {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, null, null, null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity,
            Integer ordering) {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, ordering, null, null, null,
                null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, String quantity,
            Integer ordering) {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, ordering, null, null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity,
            Integer ordering, Boolean isManual) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual) {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, ordering, directorOrdering, viewerOrdering,
                inverseViewerOrdering, isManual);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual) {

        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, ordering, directorOrdering,
                viewerOrdering, inverseViewerOrdering, isManual);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual) {

        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                inverse, includeStatementsInEvents, comment, quantity, ordering,
                directorOrdering, viewerOrdering, inverseViewerOrdering,
                isManual);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual) {
        String logInitMsg = "[addRelation] [" + session.getPrincipal().getName()
                + "] ";
        Graph graph = getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);
        QNameResource fromResource = getNodeFromDocumentModel(from);

        Resource predicateResource = new ResourceImpl(predicate);
        Statement stmt = null;
        List<Statement> statements = null;
        if (inverse) {
            stmt = new StatementImpl(toResource, predicateResource,
                    fromResource);
            statements = graph.getStatements(toResource, predicateResource,
                    fromResource);
            if (statements != null && statements.size() > 0) {
                log.error(logInitMsg + "Relation already exists. Source: |"
                        + toResource.toString() + "| Predicate: |" + predicate
                        + "| Target: |" + from.getId() + "|");
                throw new RelationAlreadyExistsException();
            }
        } else {
            stmt = new StatementImpl(fromResource, predicateResource,
                    toResource);
            statements = graph.getStatements(fromResource, predicateResource,
                    toResource);
            if (statements != null && statements.size() > 0) {
                log.error(logInitMsg + "Relation already exists. Source: |"
                        + from.getId() + "| Predicate: |" + predicate
                        + "| Target: |" + toResource.toString() + "|");
                throw new RelationAlreadyExistsException();
            }
        }

        // Comment ?
        if (!StringUtils.isEmpty(comment)) {
            stmt.addProperty(RelationConstants.COMMENT,
                    new LiteralImpl(comment));
        }
        Literal now = RelationDate.getLiteralDate(new Date());
        if (stmt.getProperties(RelationConstants.CREATION_DATE) == null) {
            stmt.addProperty(RelationConstants.CREATION_DATE, now);
        }
        if (stmt.getProperties(RelationConstants.MODIFICATION_DATE) == null) {
            stmt.addProperty(RelationConstants.MODIFICATION_DATE, now);
        }

        if (session.getPrincipal() != null
                && stmt.getProperty(RelationConstants.AUTHOR) == null) {
            stmt.addProperty(RelationConstants.AUTHOR,
                    new LiteralImpl(session.getPrincipal().getName()));
        }

        // Custom metadata for relation document model
        if (quantity != null) {
            if (stmt.getProperties(EloraRelationConstants.QUANTITY) == null) {
                stmt.addProperty(EloraRelationConstants.QUANTITY,
                        new LiteralImpl(quantity));
            }
        }

        if (ordering != null) {
            if (stmt.getProperties(EloraRelationConstants.ORDERING) == null) {
                stmt.addProperty(EloraRelationConstants.ORDERING,
                        new LiteralImpl(String.valueOf(ordering)));
            }
        }

        if (directorOrdering != null) {
            if (stmt.getProperties(
                    EloraRelationConstants.DIRECTOR_ORDERING) == null) {
                stmt.addProperty(EloraRelationConstants.DIRECTOR_ORDERING,
                        new LiteralImpl(String.valueOf(directorOrdering)));
            }
        }

        if (viewerOrdering != null) {
            if (stmt.getProperties(
                    EloraRelationConstants.VIEWER_ORDERING) == null) {
                stmt.addProperty(EloraRelationConstants.VIEWER_ORDERING,
                        new LiteralImpl(String.valueOf(viewerOrdering)));
            }
        }

        if (inverseViewerOrdering != null) {
            if (stmt.getProperties(
                    EloraRelationConstants.INVERSE_VIEWER_ORDERING) == null) {
                stmt.addProperty(EloraRelationConstants.INVERSE_VIEWER_ORDERING,
                        new LiteralImpl(String.valueOf(inverseViewerOrdering)));
            }
        }

        if (isManual != null) {
            if (stmt.getProperties(EloraRelationConstants.IS_MANUAL) == null) {
                stmt.addProperty(EloraRelationConstants.IS_MANUAL,
                        new LiteralImpl(String.valueOf(isManual)));
            }
        }

        // end of custom metadata

        // notifications

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        if (includeStatementsInEvents) {
            putStatements(options, stmt);
        }
        options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                RelationConstants.GRAPH_NAME);

        // before notification
        notifyEvent(RelationEvents.BEFORE_RELATION_CREATION, from, options,
                comment, session);

        // add statement
        graph.add(stmt);

        // XXX AT: try to refetch it from the graph so that resources are
        // transformed into qname resources: useful for indexing
        if (includeStatementsInEvents) {
            putStatements(options, graph.getStatements(stmt));
        }

        // after notification
        notifyEvent(RelationEvents.AFTER_RELATION_CREATION, from, options,
                comment, session);
    }

    @Override
    public void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo) {

        updateRelation(session, from, predicate, to, null, null, newTo, null,
                null, null, null, null, null, true);
    }

    @Override
    public void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo,
            String quantity, Integer ordering, Boolean isManual) {

        updateRelation(session, from, predicate, to, null, null, newTo,
                quantity, ordering, null, null, null, isManual, true);

    }

    @Override
    public void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo,
            String quantity, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual) {

        updateRelation(session, from, predicate, to, null, null, newTo,
                quantity, ordering, directorOrdering, viewerOrdering,
                inverseViewerOrdering, isManual, true);
    }

    @Override
    public void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newFrom,
            String newPredicate, DocumentModel newTo, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual,
            boolean checkIfNewRelationExists) {
        String logInitMsg = "[updateRelation] ["
                + session.getPrincipal().getName() + "] ";

        EloraCoreGraph graph = (EloraCoreGraph) getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);

        // Create old statement
        QNameResource fromResource = getNodeFromDocumentModel(from);
        Resource predicateResource = new ResourceImpl(predicate);
        QNameResource toResource = getNodeFromDocumentModel(to);

        Statement oldStmt = new StatementImpl(fromResource, predicateResource,
                toResource);

        // Create new statement
        Statement newStmt = (Statement) oldStmt.clone();

        if (newFrom != null) {
            newStmt.setSubject(getNodeFromDocumentModel(newFrom));
        }

        if (newPredicate != null) {
            newStmt.setPredicate(new ResourceImpl(newPredicate));
        }

        if (newTo != null) {
            newStmt.setObject(getNodeFromDocumentModel(newTo));
        }

        if (checkIfNewRelationExists) {

            // We only check if from, predicate or to has changed, else we
            // consider we only want to change properties
            if ((newFrom != null && !from.equals(newFrom))
                    || (newPredicate != null && !predicate.equals(newPredicate))
                    || (newTo != null && !to.equals(newTo))) {
                List<Statement> newStatementCopies = graph.getStatements(
                        newStmt);
                if (!newStatementCopies.isEmpty()) {
                    log.error(logInitMsg + "Relation already exists. Source: |"
                            + from.getId() + "| Predicate: |" + predicate
                            + "| Target: |" + to.getId() + "|");
                    throw new RelationAlreadyExistsException();
                }
            }
        }

        if (quantity != null) {
            newStmt.setProperty(EloraRelationConstants.QUANTITY,
                    new LiteralImpl(quantity));
        }

        if (ordering != null) {
            newStmt.setProperty(EloraRelationConstants.ORDERING,
                    new LiteralImpl(String.valueOf(ordering)));
        }

        if (directorOrdering != null) {
            newStmt.setProperty(EloraRelationConstants.DIRECTOR_ORDERING,
                    new LiteralImpl(String.valueOf(directorOrdering)));
        }

        if (viewerOrdering != null) {
            newStmt.setProperty(EloraRelationConstants.VIEWER_ORDERING,
                    new LiteralImpl(String.valueOf(viewerOrdering)));
        }

        if (inverseViewerOrdering != null) {
            newStmt.setProperty(EloraRelationConstants.INVERSE_VIEWER_ORDERING,
                    new LiteralImpl(String.valueOf(inverseViewerOrdering)));
        }

        if (isManual != null) {
            newStmt.setProperty(EloraRelationConstants.IS_MANUAL,
                    new LiteralImpl(String.valueOf(isManual)));
        }

        // Send notifications and apply the update

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        // if (includeStatementsInEvents) {
        // putStatements(options, stmt);
        // }
        options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                EloraRelationConstants.ELORA_GRAPH_NAME);

        String comment = "Changed object - old: " + to.getId() + " new: "
                + newTo.getId();

        notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from, options,
                comment, session);

        graph.update(oldStmt, newStmt);

        notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from, options,
                comment, session);

    }

    @Override
    public void softDeleteRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to) {

        EloraCoreGraph graph = (EloraCoreGraph) getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);

        // Create old statement
        QNameResource fromResource = null;
        Resource predicateResource = null;
        QNameResource toResource = null;
        if (from != null) {
            fromResource = getNodeFromDocumentModel(from);
        }
        if (predicate != null) {
            predicateResource = new ResourceImpl(predicate);
        }
        if (to != null) {
            toResource = getNodeFromDocumentModel(to);
        }

        Statement stmt = new StatementImpl(fromResource, predicateResource,
                toResource);

        // notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from,
        // options,
        // comment, session);

        graph.softDelete(stmt);

        // notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from,
        // options,
        // comment, session);

    }

    private QNameResource getNodeFromDocumentModel(DocumentModel model) {
        return (QNameResource) getRelationManager().getResource(
                RelationConstants.DOCUMENT_NAMESPACE, model, null);
    }

    // for consistency for callers only
    private static void putStatements(Map<String, Serializable> options,
            List<Statement> statements) {
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

    private static void putStatements(Map<String, Serializable> options,
            Statement statement) {
        List<Statement> statements = new LinkedList<Statement>();
        statements.add(statement);
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

}
