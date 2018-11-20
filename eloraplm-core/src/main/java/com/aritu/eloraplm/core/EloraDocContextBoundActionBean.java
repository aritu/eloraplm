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
package com.aritu.eloraplm.core;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;

/**
 * @author aritu
 *
 */
public abstract class EloraDocContextBoundActionBean implements Serializable {

    private static final Log log = LogFactory.getLog(
            EloraDocContextBoundActionBean.class);

    private static final long serialVersionUID = 1L;

    private DocumentModel currentDocument;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    protected void setCurrentDocument(DocumentModel currentDocument) {
        this.currentDocument = currentDocument;
    }

    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";
        if (doc == null) {
            currentDocument = null;
            resetBeanCache(null);
            log.trace(logInitMsg + "Document invalidated: new doc is null.");
        } else {
            if (doc.isProxy()) {
                doc = documentManager.getSourceDocument(doc.getRef());
            }

            if (currentDocument == null) {
                currentDocument = doc;
                resetBeanCache(doc);
                log.trace(logInitMsg
                        + "Document invalidated: current doc is null.");

            } else if (!doc.getRef().equals(currentDocument.getRef())) {
                currentDocument = doc;
                resetBeanCache(doc);
                log.trace(logInitMsg
                        + "Document invalidated: current and new have different reference.");
            }
        }

        return doc;
    }

    protected abstract void resetBeanCache(
            DocumentModel newCurrentDocumentModel);

}
