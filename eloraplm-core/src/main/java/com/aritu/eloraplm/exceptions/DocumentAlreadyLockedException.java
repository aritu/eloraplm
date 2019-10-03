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
 * Exception thrown when trying to lock a document that is already locked. It
 * contains the current lock owner.
 *
 * @author aritu
 *
 */
public class DocumentAlreadyLockedException extends Exception {

    private static final long serialVersionUID = 1L;

    private String owner;

    private DocumentModel doc;

    /**
     * Constructs a DocumentAlreadyLockedException with the exception message.
     *
     * @param message exception message
     */
    public DocumentAlreadyLockedException(DocumentModel doc, String owner) {
        super("Document '" + doc.getId() + "' already locked by '" + owner
                + "'.");
        this.owner = owner;
        this.doc = doc;
    }

    public String getOwner() {
        return owner;
    }

    public DocumentModel getDocument() {
        return doc;
    }

}
