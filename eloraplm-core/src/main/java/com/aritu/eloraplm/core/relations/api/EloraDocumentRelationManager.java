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
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param isObjectWc informs if relation object is a wc when version is
     *            archived
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to,
            String predicate, String comment, int quantity, boolean isObjectWc,
            int ordering);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param comment of the relation
     * @param quantity is the amount of objects
     * @param isObjectWc informs if relation object is a wc when version is
     *            archived
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, String comment, int quantity, boolean isObjectWc,
            int ordering);

    /**
     * Add link between two document
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
     * @param isObjectWc informs if relation object is a wc when version is
     *            archived
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering);

    /**
     * Add link between bom document and cad document using directorOrdering
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
     * @param isObjectWc informs if relation object is a wc when version is
     *            archived
     * @param ordering
     * @throws RelationAlreadyExistsException
     */

    public void addRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering, int directorOrdering);

    /**
     * Add link between bom document and cad document using directorOrdering
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
     * @param isObjectWc informs if relation object is a wc when version is
     *            archived
     * @param ordering
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to,
            String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment, int quantity,
            boolean isObjectWc, int ordering, int directorOrdering);

}
