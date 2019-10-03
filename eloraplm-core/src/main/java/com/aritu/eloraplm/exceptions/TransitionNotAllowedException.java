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
 * Exception thrown when trying to follow a transition that is not allowed by
 * the configuration.
 *
 * @author aritu
 *
 */
public class TransitionNotAllowedException extends Exception {

    private static final long serialVersionUID = 1L;

    private DocumentModel doc;

    private DocumentModel relatedDoc;

    private String transition;

    /**
     * @param message exception message
     */
    public TransitionNotAllowedException(DocumentModel doc, String transition) {
        super("Could not follow transition '" + transition + "' for document '"
                + doc.getId()
                + "' because one of its related documents does not support the final state.");
        this.doc = doc;
        this.transition = transition;
    }

    /**
     * @param message exception message
     */
    public TransitionNotAllowedException(DocumentModel doc,
            DocumentModel relatedDoc, String transition) {
        super("Could not follow transition '" + transition + "' for document '"
                + doc.getId() + "' because it is related to document '"
                + relatedDoc.getId()
                + "', which does not support the final state.");
        this.doc = doc;
        this.relatedDoc = relatedDoc;
        this.transition = transition;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public DocumentModel getRelatedDoc() {
        return relatedDoc;
    }

    public String getTransition() {
        return transition;
    }

}
