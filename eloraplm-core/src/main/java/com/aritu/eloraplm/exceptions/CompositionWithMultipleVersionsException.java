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
 * Exception thrown when composition has multiple versions of the same document.
 * It contains the problematic document.
 *
 * @author aritu
 *
 */
public class CompositionWithMultipleVersionsException extends Exception {

    private static final long serialVersionUID = 1L;

    private DocumentModel document;

    public CompositionWithMultipleVersionsException(DocumentModel document) {
        super("Composition with document with multiple versions '"
                + document.getId() + "'.");
        this.document = document;
    }

    public DocumentModel getDocument() {
        return document;
    }

}
