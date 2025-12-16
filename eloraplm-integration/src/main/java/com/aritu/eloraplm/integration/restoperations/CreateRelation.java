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
package com.aritu.eloraplm.integration.restoperations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;

/**
 *
 * @author aritu
 *
 */
@Operation(id = CreateRelation.ID, category = Constants.CAT_SERVICES, label = "Create EloraPLM Relation", description = "Create a relation between 2 documents with EloraPLM metadata. The subject of the relation will be the input of the operation and the object of the relation will be retrieved from the context using the 'object' field. The 'predicate' field specifies the relation predicate (When using a known predicate, use the full URL like 'purl.org/dc/terms/IsBasedOn', unknown predicates will be treated as plain strings and be the same on the subject and object). The 'outgoing' flag indicates the direction of the relation - the default is false which means the relation will go from the input object to the object specified as 'object' parameter. Return back the subject document.")
public class CreateRelation {

    public static final String ID = "Elora.Plm.CreateRelation";

    @Context
    protected CoreSession session;

    @Context
    protected EloraDocumentRelationManager relations;

    @Param(name = "object")
    protected DocumentModel object;

    @Param(name = "predicate")
    protected String predicate;

    @Param(name = "comment", required = false)
    protected String comment;

    @Param(name = "quantity", required = false)
    protected String quantity = "1";

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc)
            throws RelationNotAllowedException {
        relations.addRelation(session, doc, object, predicate, comment,
                quantity);
        return doc;
    }
}
