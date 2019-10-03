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
package com.aritu.eloraplm.cm.batchProcessing.util;

/**
 * This class encapsulates the attributes of a Result.
 *
 * @author aritu
 *
 */
public class ResultType {

    protected String documentId;

    protected String reference;

    protected String title;

    protected String message;

    /**
     * @param documentId
     * @param reference
     * @param title
     */
    public ResultType(String documentId, String reference, String title) {
        super();
        this.documentId = documentId;
        this.reference = reference;
        this.title = title;
        message = "";
    }

    /**
     * @param documentId
     * @param reference
     * @param title
     * @param message
     */
    public ResultType(String documentId, String reference, String title,
            String message) {
        super();
        this.documentId = documentId;
        this.reference = reference;
        this.title = title;
        this.message = message;
    }

    /**
     * @return the documentId
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @param documentId the documentId to set
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * @return the reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * @param reference the reference to set
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

}
