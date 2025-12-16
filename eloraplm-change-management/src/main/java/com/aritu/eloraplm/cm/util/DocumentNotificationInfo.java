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
package com.aritu.eloraplm.cm.util;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This class encapsulates the information required in a Document Notification
 *
 * @author aritu
 *
 */
public class DocumentNotificationInfo {

    private DocumentModel doc;

    private String docUrl;

    public DocumentNotificationInfo(DocumentModel doc, String docUrl) {
        super();
        this.doc = doc;
        this.docUrl = docUrl;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public void setDoc(DocumentModel doc) {
        this.doc = doc;
    }

    public String getDocUrl() {
        return docUrl;
    }

    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }

}
