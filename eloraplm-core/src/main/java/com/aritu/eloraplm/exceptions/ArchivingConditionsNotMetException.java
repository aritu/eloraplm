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
 * Exception thrown when trying to archive a workspace that does not meet the
 * required conditions.
 *
 * @author aritu
 *
 */
public class ArchivingConditionsNotMetException extends Exception {

    private static final long serialVersionUID = 1L;

    private DocumentModel doc;

    /**
     * @param message exception message
     */
    public ArchivingConditionsNotMetException(DocumentModel doc) {
        super("Could not archive the workspace '" + doc.getId()
                + "' because it does not meet the required conditions.");

        this.doc = doc;
    }

    public DocumentModel getDoc() {
        return doc;
    }

}
