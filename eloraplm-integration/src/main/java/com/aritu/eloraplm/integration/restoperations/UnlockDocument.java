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
package com.aritu.eloraplm.integration.restoperations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.versioning.EloraVersionLabelService;

/**
 * Override Nuxeo Document.Unlock so that it follows EloraPLM's rules: Nuxeo
 * standard rules + A versionable checked out document cannot be unlocked unless
 * it is a zero version
 *
 * @author aritu
 */
@Operation(id = UnlockDocument.ID, category = Constants.CAT_DOCUMENT, label = "Unlock", description = "Unlock the input document (also following EloraPLM rules). The unlock will be executed in the name of the current user. An user can unlock a document only if has the UNLOCK permission granted on the document or if it the same user as the one that locked the document. Return the unlocked document")
public class UnlockDocument {

    public static final String ID = "Document.Unlock";

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) {
        if (canUnlock(doc)) {
            session.removeLock(doc);
        }
        return session.getDocument(doc);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (canUnlock(doc)) {
            session.removeLock(doc.getRef());
        }
        return session.getDocument(doc.getRef());
    }

    private boolean canUnlock(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        if (doc != null) {
            return canUnlock(doc);
        }
        return false;
    }

    private boolean canUnlock(DocumentModel doc) {
        // TODO PowerUsers?
        EloraVersionLabelService evls = Framework.getService(
                EloraVersionLabelService.class);
        if (doc.isVersionable() && doc.isCheckedOut()
                && !doc.getVersionLabel().equals(evls.getZeroVersion())) {
            return false;
        }

        return true;
    }

}
