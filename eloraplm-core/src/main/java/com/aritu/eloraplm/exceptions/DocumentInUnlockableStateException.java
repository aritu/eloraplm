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
 * Exception thrown when the current lifecycle state of the document is
 * configured as not lockable. It contains the current lifecycle state.
 *
 * @author aritu
 *
 */
public class DocumentInUnlockableStateException extends Exception {

    private static final long serialVersionUID = 1L;

    private String currentLifeCycleState;

    private DocumentModel doc;

    /**
     * Constructs a DocumentInUnlockableStateException with the exception
     * message.
     *
     * @param message exception message
     */
    public DocumentInUnlockableStateException(DocumentModel doc,
            String currentLifeCycleState) {
        super("Document '" + doc.getId() + "' is in lifecycle state '"
                + currentLifeCycleState + "' and is not lockable.");
        this.currentLifeCycleState = currentLifeCycleState;
        this.doc = doc;
    }

    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    public DocumentModel getDocument() {
        return doc;
    }

}
