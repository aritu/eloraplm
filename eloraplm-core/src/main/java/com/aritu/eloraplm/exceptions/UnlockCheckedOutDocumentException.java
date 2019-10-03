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
 * Exception thrown when trying to unlock a document that is checked out.
 *
 * @author aritu
 *
 */
public class UnlockCheckedOutDocumentException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnlockCheckedOutDocumentException(DocumentModel doc) {
        super("Document '" + doc.getId()
                + "' cannot be unlocked because it is checked out, versionable, and not in zero version.");
    }
}
