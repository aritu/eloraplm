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

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;

/**
 * @author aritu
 *
 */
public abstract class EloraDocContextBoundActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private DocumentModel currentDocument;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    @DocumentContextInvalidation
    public void onContextChange(DocumentModel doc) {
        if (doc == null) {
            currentDocument = null;
            resetBeanCache(null);
            return;
        } else {
            if (doc.isProxy()) {
                doc = documentManager.getSourceDocument(doc.getRef());
            }

            if (currentDocument == null) {
                currentDocument = doc;
                resetBeanCache(doc);
                return;
            }
        }
        if (!doc.getRef().equals(currentDocument.getRef())) {
            currentDocument = doc;
            resetBeanCache(doc);
        }
        if (!(doc.getLockInfo() == currentDocument.getLockInfo())
                || !(doc.isCheckedOut() == currentDocument.isCheckedOut())) {
            currentDocument = doc;
            resetBeanCache(doc);
        }
    }

    protected abstract void resetBeanCache(DocumentModel newCurrentDocumentModel);

}
