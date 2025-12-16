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
package com.aritu.eloraplm.exceptions;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Exception thrown when we are trying to create a relation that has source
 * type, predicate & target type that is not allowed by the
 * RelationManagementService
 *
 * @author aritu
 *
 */
public class RelationNotAllowedException extends EloraException {

    private static final long serialVersionUID = 1L;

    public RelationNotAllowedException(DocumentModel source,
            String predicateUri, DocumentModel target) {
        super("Relation not allowed! Source type: '" + source.getType()
                + "', Predicate: '" + predicateUri + "', Target type: '"
                + target.getType()
                + "'. Maybe it needs to be registered in allowedRelations XP.");
    }
}
