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
package com.aritu.eloraplm.core.relations.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;

import com.aritu.eloraplm.core.relations.RelationGraph;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public interface RelationManagementService {

    /**
     * Check if relation is allowed
     *
     * @param source
     * @param predicateUri
     * @param target
     * @return
     */
    public boolean isRelationAllowed(DocumentModel source, String predicateUri,
            DocumentModel target);

    /**
     * Add link between two documents
     *
     * @param session
     * @param graph
     * @param from the document to link from
     * @param predicate is the type of link
     * @param to the document to link to
     * @param inverse if to is related to from ( the event will still be
     *            generated with from document )
     * @param includeStatementsInEvents will add the statement to the events
     *            RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @param comment of the relation
     * @param properties
     * @throws RelationAlreadyExistsException
     * @throws RelationNotAllowedException
     */
    public void addRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to,
            boolean inverse, boolean includeStatementsInEvents, String comment,
            Map<Resource, Node> properties)
            throws RelationAlreadyExistsException, RelationNotAllowedException;

    /**
     * Update relation's object and properties
     *
     * @param session
     * @param graph
     * @param from the document to link from
     * @param predicate is the type of link
     * @param to the document to link to
     * @param propertiesToUpdate
     * @param includeStatementsInEvents
     */
    void updateRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to,
            Map<Resource, Node> propertiesToUpdate,
            boolean includeStatementsInEvents) throws EloraException;

    /**
     * Mark relation to remove later, updating from, predicate and to fields
     *
     * @param session
     * @param graph
     * @param from
     * @param predicate
     * @param to
     */
    void softDeleteRelation(CoreSession session, RelationGraph graph,
            DocumentModel from, String predicate, DocumentModel to);

    /**
     * Mark relation to remove later by statement, updating from, predicate and
     * to fields
     *
     * @param session
     * @param graph
     * @param stmt
     */
    void softDeleteRelation(CoreSession session, RelationGraph graph,
            Statement stmt);

}
