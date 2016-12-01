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
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraDocumentRelationService extends DocumentRelationService
        implements EloraDocumentRelationManager {

    // TODO: Los campos personales se pasan como string porque por ahora no
    // sabemos gestionarlos si pasamos integer, boolean etc. Esto es debido a la
    // clase Literal de nuxeo

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, int quantity,
            boolean isObjectWc, int ordering) {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, isObjectWc, ordering);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, int quantity,
            boolean isObjectWc, int ordering) {

        addRelation(session, from, toResource, predicate, false, false,
                comment, quantity, isObjectWc, ordering);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering) {

        addRelation(session, from, toResource, predicate, inverse,
                includeStatementsInEvents, comment, quantity, isObjectWc,
                ordering, 0);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering, int directorOrdering) {

        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                inverse, includeStatementsInEvents, comment, quantity,
                isObjectWc, ordering, 0);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering, int directorOrdering) {
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
                throw new RelationAlreadyExistsException();
            }
        } else {
            stmt = new StatementImpl(fromResource, predicateResource,
                    toResource);
            statements = graph.getStatements(fromResource, predicateResource,
                    toResource);
            if (statements != null && statements.size() > 0) {
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
            stmt.addProperty(RelationConstants.AUTHOR, new LiteralImpl(
                    session.getPrincipal().getName()));
        }

        // Custom metadata for relation document model
        if (stmt.getProperties(EloraRelationConstants.QUANTITY) == null) {
            stmt.addProperty(EloraRelationConstants.QUANTITY, new LiteralImpl(
                    String.valueOf(quantity)));
        }

        if (stmt.getProperties(EloraRelationConstants.IS_OBJECT_WC) == null) {
            stmt.addProperty(EloraRelationConstants.IS_OBJECT_WC,
                    new LiteralImpl(String.valueOf(isObjectWc)));
        }

        if (stmt.getProperties(EloraRelationConstants.ORDERING) == null) {
            stmt.addProperty(EloraRelationConstants.ORDERING, new LiteralImpl(
                    String.valueOf(ordering)));
        }

        if (stmt.getProperties(EloraRelationConstants.DIRECTOR_ORDERING) == null) {
            stmt.addProperty(EloraRelationConstants.DIRECTOR_ORDERING,
                    new LiteralImpl(String.valueOf(directorOrdering)));
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
