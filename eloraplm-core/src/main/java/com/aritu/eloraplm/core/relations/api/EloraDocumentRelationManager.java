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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public interface EloraDocumentRelationManager extends DocumentRelationManager {

    /**
     * Add link between two documents (basic, only with quantity)
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to,
            String predicate, String comment, String quantity);

    /**
     * Add link between two documents (basic, only with quantity)
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, String comment, String quantity);

    /**
     * Add link between two documents with quantity and ordering
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to,
            String predicate, String comment, String quantity,
            Integer ordering);

    /**
     * Add link between two document with quantity and ordering
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, String comment, String quantity,
            Integer ordering);

    /**
     * Add link between two document with quantity and all orderings
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, String comment, String quantity, Integer ordering,
            Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering);

    /**
     * Add link between two document with quantity and all orderings
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to,
            String predicate, String comment, String quantity, Integer ordering,
            Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering);

    /**
     * Add inverse link between two documents (all parameters and properties)
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be
     *            generated with from document )
     * @param includeStatementsInEvents will add the statement to the events
     *            RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering);

    /**
     * Add link between two documents (all parameters and properties)
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be
     *            generated with from document )
     * @param includeStatementsInEvents will add the statement to the events
     *            RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @throws RelationAlreadyExistsException
     */

    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering);

    /**
     * Update relation's object
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param newTo the updated document to link to
     */
    void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo);

    /**
     * Update relation's object and properties
     *
     * @param from the document to link from
     * @param predicate is the type of link
     * @param to the document to link to
     * @param newTo the updated document to link to
     * @param quantity
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     */
    void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newTo,
            String quantity, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering);

    /**
     * Update relation, base method
     *
     * @param from the document to link from
     * @param predicate is the type of link
     * @param to the document to link to
     * @param newFrom the updated document to link from
     * @param newPredicate is the updated type of link
     * @param newTo the updated document to link to
     * @param quantity
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @param checkIfNewRelationExists
     */
    void updateRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to, DocumentModel newFrom,
            String newPredicate, DocumentModel newTo, String quantity,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, boolean checkIfNewRelationExists);

    /**
     * Mark relation to remove later, updating from, predicate and to fields
     *
     * @param session
     * @param from
     * @param predicate
     * @param to
     */
    void softDeleteRelation(CoreSession session, DocumentModel from,
            String predicate, DocumentModel to);

}
