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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.RelationGraph;
import com.aritu.eloraplm.core.relations.api.AllowedRelationDescriptor;
import com.aritu.eloraplm.core.relations.api.AllowedRelationInfo;
import com.aritu.eloraplm.core.relations.api.AllowedRelationSourceOrTargetDescriptor;
import com.aritu.eloraplm.core.relations.api.AllowedRelationSourceOrTargetTypeDescriptor;
import com.aritu.eloraplm.core.relations.api.RelationManagementService;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class RelationManagementServiceImpl extends DefaultComponent
        implements RelationManagementService {

    private static final Log log = LogFactory.getLog(
            RelationManagementServiceImpl.class);

    private static final String XP_ALLOWED_RELATIONS = "allowedRelations";

    private List<AllowedRelationDescriptor> registeredDescriptors;

    private List<UUID> allowedRelations;

    private RelationManager relationManager = null;

    /* ********************************** *
     * EXTENSION POINTS                   *
     * ********************************** */

    @Override
    public void activate(ComponentContext context) {
        allowedRelations = null;
        registeredDescriptors = new ArrayList<AllowedRelationDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        allowedRelations = null;
        registeredDescriptors = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_ALLOWED_RELATIONS:
            AllowedRelationDescriptor ard = (AllowedRelationDescriptor) contribution;
            if (ard.predicateUri != null) {
                AllowedRelationSourceOrTargetDescriptor sd = ard.source;
                AllowedRelationSourceOrTargetDescriptor td = ard.target;
                if (sd == null) {
                    throw new NuxeoException("Relation source is null");
                }
                if (td == null) {
                    throw new NuxeoException("Relation target is null");
                }

                // Store as registered, real list will be filled later
                registeredDescriptors.add(ard);

            } else {
                throw new NuxeoException("Relation predicate is null");
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    // We wanted to init the allowed relations list when we register the
    // contributions, but on servers, SchemaManager isn't created until later,
    // so, for now, we lazy load on the first use
    private void initAllowedRelations() {
        allowedRelations = new ArrayList<UUID>();

        for (AllowedRelationDescriptor ard : registeredDescriptors) {
            AllowedRelationSourceOrTargetDescriptor sd = ard.source;
            AllowedRelationSourceOrTargetDescriptor td = ard.target;

            Set<String> sourceTypes = new LinkedHashSet<String>();
            String predicateUri = ard.predicateUri;
            Set<String> targetTypes = new LinkedHashSet<String>();

            for (AllowedRelationSourceOrTargetTypeDescriptor std : sd.types) {
                sourceTypes.addAll(treatDeclaredType(std));
            }
            for (String sf : sd.facets) {
                sourceTypes.addAll(treatDeclaredFacet(sf));
            }
            for (AllowedRelationSourceOrTargetTypeDescriptor ttd : td.types) {
                targetTypes.addAll(treatDeclaredType(ttd));
            }
            for (String tf : td.facets) {
                targetTypes.addAll(treatDeclaredFacet(tf));
            }

            for (String sourceType : sourceTypes) {
                for (String targetType : targetTypes) {
                    AllowedRelationInfo ari = new AllowedRelationInfo(
                            sourceType, predicateUri, targetType);
                    log.trace(
                            "Registering allowed relation: " + ari.toString());

                    // Create UUID to add to list
                    UUID ariUuid = UUID.nameUUIDFromBytes(
                            ari.toString().getBytes());
                    if (!allowedRelations.contains(ariUuid)) {
                        allowedRelations.add(ariUuid);
                    }
                }
            }

        }
    }

    private List<String> treatDeclaredType(
            AllowedRelationSourceOrTargetTypeDescriptor td) {
        List<String> types = new ArrayList<String>();
        String declaredType = td.value;
        if (!td.excludeInherited) {
            types.addAll(EloraDocumentTypesHelper.getExtendedDocumentTypeNames(
                    declaredType));
        } else {
            types.add(declaredType);
        }
        return types;
    }

    private List<String> treatDeclaredFacet(String facet) {
        return EloraDocumentTypesHelper.getTypeNamesForFacet(facet);
    }

    @Override
    public boolean isRelationAllowed(DocumentModel source, String predicateUri,
            DocumentModel target) {
        // We must load allowed relations the first time they are used to avoid
        // SchemaManager not being loaded
        if (allowedRelations == null) {
            initAllowedRelations();
        }
        AllowedRelationInfo ari = new AllowedRelationInfo(source.getType(),
                predicateUri, target.getType());
        UUID ariUuid = UUID.nameUUIDFromBytes(ari.toString().getBytes());
        if (allowedRelations.contains(ariUuid)) {
            return true;
        }
        return false;
    }

    /* ********************************** *
     *  ADD RELATION                      *
     * ********************************** */

    @Override
    public void addRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to,
            boolean inverse, boolean includeStatementsInEvents, String comment,
            Map<Resource, Node> properties)
            throws RelationAlreadyExistsException, RelationNotAllowedException {
        String logInitMsg = "[addRelation] [" + session.getPrincipal().getName()
                + "] ";

        // Apply inverse parameter
        DocumentModel subject = inverse ? to : from;
        DocumentModel object = inverse ? from : to;

        // Check if relation is allowed
        boolean relationAllowed = isRelationAllowed(subject, predicate, object);
        if (!relationAllowed) {
            throw new RelationNotAllowedException(subject, predicate, object);
        }

        QNameResource subjectResource = getNodeFromDocumentModel(subject);
        Resource predicateResource = new ResourceImpl(predicate);
        QNameResource objectResource = getNodeFromDocumentModel(object);

        Statement stmt = null;
        List<Statement> statements = null;

        stmt = new StatementImpl(subjectResource, predicateResource,
                objectResource);
        statements = graph.getStatements(subjectResource, predicateResource,
                objectResource);
        if (statements != null && statements.size() > 0) {
            log.error(logInitMsg + "Relation already exists. Source: |"
                    + subject.getId() + "| Predicate: |" + predicate
                    + "| Target: |" + object.getId() + "|");
            throw new RelationAlreadyExistsException();
        }

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

        // Set isTemplate (if not already set) based on the subject
        Resource isTemplateRes = graph.getMetadataResourceMap().get(
                EloraMetadataConstants.ELORA_RELEXT_ISTEMPLATE);
        if (stmt.getProperties(isTemplateRes) == null) {
            if (EloraDocumentHelper.isTemplate(from)) {
                stmt.addProperty(isTemplateRes,
                        new LiteralImpl(String.valueOf(true)));
            } else {
                stmt.addProperty(isTemplateRes,
                        new LiteralImpl(String.valueOf(false)));
            }
        }

        // add custom properties
        for (Map.Entry<Resource, Node> prp : properties.entrySet()) {
            if (stmt.getProperties(prp.getKey()) == null) {
                stmt.addProperty(prp.getKey(), prp.getValue());
            }
        }

        // notifications
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        if (includeStatementsInEvents) {
            putStatements(options, stmt);
        }
        // TODO Ezin dugu graphetik izena atera - Zertarako balio du?
        // options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
        // RelationConstants.GRAPH_NAME);

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

    /* ********************************** *
     *  UPDATE RELATION                   *
     * ********************************** */

    @Override
    public void updateRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to,
            Map<Resource, Node> propertiesToUpdate,
            boolean includeStatementsInEvents) throws EloraException {
        String logInitMsg = "[updateRelation] ["
                + session.getPrincipal().getName() + "] ";

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
        if (propertiesToUpdate.containsKey(
                EloraRelationConstants.RES_SUBJECT)) {
            newStmt.setSubject(
                    propertiesToUpdate.get(EloraRelationConstants.RES_SUBJECT));
            propertiesToUpdate.remove(EloraRelationConstants.RES_SUBJECT);
        }
        if (propertiesToUpdate.containsKey(
                EloraRelationConstants.RES_PREDICATE)) {
            newStmt.setPredicate(propertiesToUpdate.get(
                    EloraRelationConstants.RES_PREDICATE));
            propertiesToUpdate.remove(EloraRelationConstants.RES_PREDICATE);
        }
        if (propertiesToUpdate.containsKey(EloraRelationConstants.RES_OBJECT)) {
            newStmt.setObject(
                    propertiesToUpdate.get(EloraRelationConstants.RES_OBJECT));
            propertiesToUpdate.remove(EloraRelationConstants.RES_OBJECT);
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
        for (Map.Entry<Resource, Node> prp : propertiesToUpdate.entrySet()) {

            // We only treat literal properties

            Node prpNode = prp.getValue();
            if (prpNode.isLiteral()) {
                LiteralImpl litPrp = (LiteralImpl) prpNode;
                if (litPrp.getValue() != null) {
                    newStmt.setProperty(prp.getKey(), prp.getValue());
                } else {
                    newStmt.deleteProperty(prp.getKey());
                }
            }
        }

        // Send notifications and apply the update
        Map<String, Serializable> options = new HashMap<String, Serializable>();

        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);

        // TODO Ezin dugu graphetik izena atera - Zertarako balio du?
        // options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
        // graph.EloraRelationConstants.ELORA_GRAPH_NAME);

        if (includeStatementsInEvents) {
            putStatements(options, newStmt);
        }

        String comment = "Changed relation - old: " + from.getId() + " ("
                + predicate + ") " + to.getId() + " New properties: "
                + propertiesToUpdate.toString();

        notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from, options,
                comment, session);

        graph.update(oldStmt, newStmt);

        notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from, options,
                comment, session);

    }

    /* ********************************** *
     *  DELETE RELATION                   *
     * ********************************** */

    @Override
    public void softDeleteRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to) {

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

        softDeleteRelation(session, graph, stmt);

    }

    @Override
    public void softDeleteRelation(CoreSession session, RelationGraph graph,
            Statement stmt) {

        // TODO Ebentu berezi bat?
        // notifyEvent(RelationEvents.BEFORE_RELATION_MODIFICATION, from,
        // options, comment, session);

        graph.softDelete(stmt);

        // TODO Ebentu berezi bat?
        // notifyEvent(RelationEvents.AFTER_RELATION_MODIFICATION, from,
        // options, comment, session);

    }

    /* ********************************** *
     *  COMMON FUNCTIONS                  *
     * ********************************** */

    protected RelationManager getRelationManager() {
        if (relationManager == null) {
            relationManager = Framework.getLocalService(RelationManager.class);
        }
        return relationManager;
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

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Serializable> options, String comment,
            CoreSession session) {
        DocumentEventContext docCtx = new DocumentEventContext(session,
                session.getPrincipal(), source);
        options.put("category", RelationEvents.CATEGORY);
        options.put("comment", comment);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        evtProducer.fireEvent(docCtx.newEvent(eventId));
    }

}
