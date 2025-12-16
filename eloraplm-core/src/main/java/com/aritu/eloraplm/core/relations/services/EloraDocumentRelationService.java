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
import java.util.ArrayList;
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
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.services.DocumentRelationService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.CommonRelationConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.EloraCoreGraph;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.api.PredicateList;
import com.aritu.eloraplm.core.relations.api.RelationManagementService;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;

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

    private static final String PROP_SUBJECT = "subject";

    private static final String PROP_PREDICATE = "predicate";

    private static final String PROP_OBJECT = "object";

    private static final String PROP_QUANTITY = "quantity";

    private static final String PROP_ORDERING = "ordering";

    private static final String PROP_DIRECTOR_ORDERING = "directorOrdering";

    private static final String PROP_VIEWER_ORDERING = "viewerOrdering";

    private static final String PROP_INVERSE_VIEWER_ORDERING = "inverseViewerOrdering";

    private static final String PROP_IS_MANUAL = "isManual";

    private static final String PROP_DESIGNATORS = "designators";

    // Common relation
    private static final String PROP_IS_EXCLUDED_FROM_MAPPING = "isExcludedFromMapping";

    /* **********************************
     *  ADD RELATION
     * ********************************** */

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity)
            throws RelationNotAllowedException {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, null, null, null, null, null,
                null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, String quantity)
            throws RelationNotAllowedException {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, null, null, null, null, null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity,
            Integer ordering) throws RelationNotAllowedException {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, ordering, null, null, null,
                null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, String quantity,
            Integer ordering) throws RelationNotAllowedException {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, ordering, null, null, null, null, null, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual, String designators,
            Boolean isExcludedFromMapping) throws RelationNotAllowedException {

        addRelation(session, from, toResource, predicate, false, false, comment,
                quantity, ordering, directorOrdering, viewerOrdering,
                inverseViewerOrdering, isManual, designators,
                isExcludedFromMapping);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual, String designators,
            Boolean isExcludedFromMapping) throws RelationNotAllowedException {

        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                false, false, comment, quantity, ordering, directorOrdering,
                viewerOrdering, inverseViewerOrdering, isManual, designators,
                isExcludedFromMapping);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual, String designators,
            Boolean isExcludedFromMapping) throws RelationNotAllowedException {

        addRelation(session, from, getNodeFromDocumentModel(to), predicate,
                inverse, includeStatementsInEvents, comment, quantity, ordering,
                directorOrdering, viewerOrdering, inverseViewerOrdering,
                isManual, designators, isExcludedFromMapping);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual, String designators,
            Boolean isExcludedFromMapping) throws RelationNotAllowedException {
        String logInitMsg = "[addRelation] [" + session.getPrincipal().getName()
                + "] ";
        EloraCoreGraph graph = (EloraCoreGraph) getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);

        DocumentModel to = RelationHelper.getDocumentModel(toResource, session);

        // Apply inverse parameter
        DocumentModel subject = inverse ? to : from;
        DocumentModel object = inverse ? from : to;

        // Check if relation is allowed
        RelationManagementService rms = Framework.getService(
                RelationManagementService.class);
        boolean relationAllowed = rms.isRelationAllowed(subject, predicate,
                object);
        if (!relationAllowed) {
            throw new RelationNotAllowedException(subject, predicate, object);
        }

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
            Resource quantityRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_QUANTITY);
            if (stmt.getProperties(quantityRes) == null) {
                stmt.addProperty(quantityRes, new LiteralImpl(quantity));
            }
        }

        if (ordering != null) {
            Resource orderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_ORDERING);
            if (stmt.getProperties(orderingRes) == null) {
                stmt.addProperty(orderingRes,
                        new LiteralImpl(String.valueOf(ordering)));
            }
        }

        if (directorOrdering != null) {
            Resource directorOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING);
            if (stmt.getProperties(directorOrderingRes) == null) {
                stmt.addProperty(directorOrderingRes,
                        new LiteralImpl(String.valueOf(directorOrdering)));
            }
        }

        if (viewerOrdering != null) {
            Resource viewerOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING);
            if (stmt.getProperties(viewerOrderingRes) == null) {
                stmt.addProperty(viewerOrderingRes,
                        new LiteralImpl(String.valueOf(viewerOrdering)));
            }
        }

        if (inverseViewerOrdering != null) {
            Resource inverseViewerOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING);
            if (stmt.getProperties(inverseViewerOrderingRes) == null) {
                stmt.addProperty(inverseViewerOrderingRes,
                        new LiteralImpl(String.valueOf(inverseViewerOrdering)));
            }
        }

        if (isManual != null) {
            Resource isManualRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_ISMANUAL);
            if (stmt.getProperties(isManualRes) == null) {
                stmt.addProperty(isManualRes,
                        new LiteralImpl(String.valueOf(isManual)));
            }
        }

        if (designators != null) {
            Resource designatorsRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_DESIGNATORS);
            if (stmt.getProperties(designatorsRes) == null) {
                stmt.addProperty(designatorsRes, new LiteralImpl(designators));
            }
        }

        if (EloraDocumentHelper.isTemplate(from)) {
            Resource isTemplateRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_ISTEMPLATE);
            if (stmt.getProperties(isTemplateRes) == null) {
                stmt.addProperty(isTemplateRes,
                        new LiteralImpl(String.valueOf(true)));
            }
        }

        // end of custom metadata

        // Common relation metadata

        if (isExcludedFromMapping != null) {
            Resource isExcludedFromMappingRes = graph.getMetadataResourceMap().get(
                    CommonRelationConstants.COMMREL_IS_EXCLUDED_FROM_MAPPING);
            if (stmt.getProperties(isExcludedFromMappingRes) == null) {
                stmt.addProperty(isExcludedFromMappingRes,
                        new LiteralImpl(String.valueOf(isExcludedFromMapping)));
            }
        }

        // End of common relation metadata

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

    /* **********************************
     *  UPDATE RELATION
     * ********************************** */

    @Override
    public void updateObjectInRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo)
            throws EloraException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_OBJECT, newTo);

        updateRelation(session, from, predicate, to, props);
    }

    @Override
    public void updateObjectAndBasicPropsInRelation(CoreSession session,
            DocumentModel from, String predicate, DocumentModel to,
            DocumentModel newTo, String quantity, Integer ordering)
            throws EloraException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_OBJECT, newTo);
        props.put(PROP_QUANTITY, quantity);
        props.put(PROP_ORDERING, ordering);

        updateRelation(session, from, predicate, to, props);
    }

    @Override
    public void updateObjectAndAllPropsInRelation(CoreSession session,
            DocumentModel from, String predicate, DocumentModel to,
            DocumentModel newTo, String quantity, Integer ordering,
            Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, Boolean isManual, String designators,
            Boolean isExcludedFromMapping) throws EloraException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_OBJECT, newTo);
        props.put(PROP_QUANTITY, quantity);
        props.put(PROP_ORDERING, ordering);
        props.put(PROP_DIRECTOR_ORDERING, directorOrdering);
        props.put(PROP_VIEWER_ORDERING, viewerOrdering);
        props.put(PROP_INVERSE_VIEWER_ORDERING, inverseViewerOrdering);
        props.put(PROP_IS_MANUAL, isManual);
        props.put(PROP_DESIGNATORS, designators);

        props.put(PROP_IS_EXCLUDED_FROM_MAPPING, isExcludedFromMapping);

        updateRelation(session, from, predicate, to, props);
    }

    @Override
    public void updateQuantityAndOrderingInRelation(CoreSession session,
            DocumentModel from, String predicate, DocumentModel to,
            String quantity, Integer ordering) throws EloraException {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_QUANTITY, quantity);
        props.put(PROP_ORDERING, ordering);
        updateRelation(session, from, predicate, to, props);
    }

    private void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, Map<String, Object> props)
            throws EloraException {
        String logInitMsg = "[updateRelation] ["
                + session.getPrincipal().getName() + "] ";

        EloraCoreGraph graph = (EloraCoreGraph) getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);

        // Create old statement
        QNameResource fromResource = getNodeFromDocumentModel(from);
        Resource predicateResource = new ResourceImpl(predicate);
        QNameResource toResource = getNodeFromDocumentModel(to);

        List<Statement> oldStmts = graph.getStatements(fromResource,
                predicateResource, toResource);
        if (oldStmts.size() != 1) {
            log.error(logInitMsg
                    + "There is no relation with provided data (or more than one exists). predicateUri = |"
                    + predicate + "|, subject docUid = |" + from.getId()
                    + "|, object docUid = |" + to.getId()
                    + "|. Normally THIS HAPPENS BECAUSE WC HAS RELATIONS TO AV DOCS or WC AND BASE VERSION HAVE DIFFERENT OBJECTS RELATED when it is not checked out. Must fix relations from DB.");
            throw new EloraException(
                    "No relation exists with provided data. Non-checked-out document with relations from WC to AV or different objects related in WC and base version?");
        }
        Statement oldStmt = oldStmts.get(0);

        Statement newStmt = (Statement) oldStmt.clone();

        // Subject, Predicate & Object

        if (props.containsKey(PROP_SUBJECT)) {
            newStmt.setSubject(getNodeFromDocumentModel(
                    (DocumentModel) props.get(PROP_SUBJECT)));
        }
        if (props.containsKey(PROP_PREDICATE)) {
            newStmt.setPredicate(
                    new ResourceImpl((String) props.get(PROP_PREDICATE)));
        }
        if (props.containsKey(PROP_OBJECT)) {
            newStmt.setObject(getNodeFromDocumentModel(
                    (DocumentModel) props.get(PROP_OBJECT)));
        }

        // If subject, predicate or object has changed,
        // check that the new relation does not already exist
        if (!oldStmt.getSubject().equals(newStmt.getSubject())
                || !oldStmt.getPredicate().equals(newStmt.getPredicate())
                || !oldStmt.getObject().equals(newStmt.getObject())) {

            if (!graph.getStatements(newStmt).isEmpty()) {
                log.error(logInitMsg + "Relation already exists. Source: |"
                        + newStmt.getSubject().toString() + "| Predicate: |"
                        + newStmt.getPredicate().getUri() + "| Target: |"
                        + newStmt.getObject().toString() + "|");
                throw new RelationAlreadyExistsException();
            }
        }

        // Check the other properties

        if (props.containsKey(PROP_QUANTITY)) {
            String quantity = (String) props.get(PROP_QUANTITY);
            Resource quantityRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_QUANTITY);
            if (quantity != null) {
                newStmt.setProperty(quantityRes, new LiteralImpl(quantity));
            } else {
                newStmt.deleteProperty(quantityRes);
            }
        }

        if (props.containsKey(PROP_ORDERING)) {
            Integer ordering = (Integer) props.get(PROP_ORDERING);
            Resource orderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_ORDERING);
            if (ordering != null) {
                newStmt.setProperty(orderingRes,
                        new LiteralImpl(String.valueOf(ordering)));
            } else {
                newStmt.deleteProperty(orderingRes);
            }
        }

        if (props.containsKey(PROP_DIRECTOR_ORDERING)) {
            Integer directorOrdering = (Integer) props.get(
                    PROP_DIRECTOR_ORDERING);
            Resource directorOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING);
            if (directorOrdering != null) {
                newStmt.setProperty(directorOrderingRes,
                        new LiteralImpl(String.valueOf(directorOrdering)));
            } else {
                newStmt.deleteProperty(directorOrderingRes);
            }
        }

        if (props.containsKey(PROP_VIEWER_ORDERING)) {
            Integer viewerOrdering = (Integer) props.get(PROP_VIEWER_ORDERING);
            Resource viewerOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING);
            if (viewerOrdering != null) {
                newStmt.setProperty(viewerOrderingRes,
                        new LiteralImpl(String.valueOf(viewerOrdering)));
            } else {
                newStmt.deleteProperty(viewerOrderingRes);
            }
        }

        if (props.containsKey(PROP_INVERSE_VIEWER_ORDERING)) {
            Integer inverseViewerOrdering = (Integer) props.get(
                    PROP_INVERSE_VIEWER_ORDERING);
            Resource inverseViewerOrderingRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING);
            if (inverseViewerOrdering != null) {
                newStmt.setProperty(inverseViewerOrderingRes,
                        new LiteralImpl(String.valueOf(inverseViewerOrdering)));
            } else {
                newStmt.deleteProperty(inverseViewerOrderingRes);
            }
        }

        if (props.containsKey(PROP_IS_MANUAL)) {
            Boolean isManual = (Boolean) props.get(PROP_IS_MANUAL);
            Resource isManualRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_ISMANUAL);
            if (isManual != null) {
                newStmt.setProperty(isManualRes,
                        new LiteralImpl(String.valueOf(isManual)));
            } else {
                newStmt.deleteProperty(isManualRes);
            }
        }

        if (props.containsKey(PROP_DESIGNATORS)) {
            String designators = (String) props.get(PROP_DESIGNATORS);
            Resource designatorsRes = graph.getMetadataResourceMap().get(
                    EloraMetadataConstants.ELORA_RELEXT_DESIGNATORS);
            if (designators != null) {
                newStmt.setProperty(designatorsRes,
                        new LiteralImpl(designators));
            } else {
                newStmt.deleteProperty(designatorsRes);
            }
        }

        if (props.containsKey(PROP_IS_EXCLUDED_FROM_MAPPING)) {
            Boolean isExcludedFromMapping = (Boolean) props.get(
                    PROP_IS_EXCLUDED_FROM_MAPPING);
            Resource isExcludedFromMappingRes = graph.getMetadataResourceMap().get(
                    CommonRelationConstants.COMMREL_IS_EXCLUDED_FROM_MAPPING);
            if (isExcludedFromMapping != null) {
                newStmt.setProperty(isExcludedFromMappingRes,
                        new LiteralImpl(String.valueOf(isExcludedFromMapping)));
            } else {
                newStmt.deleteProperty(isExcludedFromMappingRes);
            }
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

        String comment = "Changed relation - old: " + from.getId() + " ("
                + predicate + ") " + to.getId() + " New properties: "
                + props.toString();

        notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from, options,
                comment, session);

        graph.update(oldStmt, newStmt);

        notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from, options,
                comment, session);

    }

    /* **********************************
     *  DELETE RELATION
     * ********************************** */

    @Override
    public void softDeleteRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to) {

        // Create old statement
        QNameResource fromResource = null;
        QNameResource toResource = null;
        if (from != null) {
            fromResource = getNodeFromDocumentModel(from);
        }
        Resource predicateResource = null;
        if (predicate != null) {
            predicateResource = new ResourceImpl(predicate);
        }
        if (to != null) {
            toResource = getNodeFromDocumentModel(to);
        }

        softDeleteRelation(session, fromResource, predicateResource,
                toResource);

    }

    @Override
    public void softDeleteRelation(CoreSession session, DocumentModel from,
            List<String> predicates, DocumentModel to) {

        // Create old statement
        QNameResource fromResource = null;
        QNameResource toResource = null;
        if (from != null) {
            fromResource = getNodeFromDocumentModel(from);
        }
        PredicateList predicatesResource = null;
        if (predicates != null) {
            List<Resource> predicateList = new ArrayList<Resource>();
            for (String predicate : predicates) {
                predicateList.add(new ResourceImpl(predicate));
            }
            predicatesResource = new PredicateList(predicateList);
        }
        if (to != null) {
            toResource = getNodeFromDocumentModel(to);
        }

        softDeleteRelation(session, fromResource, predicatesResource,
                toResource);

    }

    @Override
    public void softDeleteRelation(CoreSession session, QNameResource from,
            Resource predicate, QNameResource to) {

        EloraCoreGraph graph = (EloraCoreGraph) getRelationManager().getGraph(
                EloraRelationConstants.ELORA_GRAPH_NAME, session);

        Statement stmt = new StatementImpl(from, predicate, to);

        // notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from,
        // options,
        // comment, session);

        graph.softDelete(stmt);

        // notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from,
        // options,
        // comment, session);

    }

    /* **********************************
     *  COMMON FUNCTIONS
     * ********************************** */

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
