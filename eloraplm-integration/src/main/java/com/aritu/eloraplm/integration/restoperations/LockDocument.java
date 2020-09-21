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
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Override Nuxeo Document.Lock so that it follows EloraPLM's rules: Nuxeo
 * standard rules + current state must be lockable
 *
 * @author aritu
 */
@Operation(id = LockDocument.ID, category = Constants.CAT_DOCUMENT, label = "Lock", description = "Lock the input document for the current user (also following EloraPLM rules). Returns back the locked document.")
public class LockDocument {

    public static final String ID = "Document.Lock";

    @Context
    protected CoreSession session;

    /** @deprecated unused */
    @Deprecated
    @Param(name = "owner", required = false)
    protected String owner;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) {
        if (canLock(doc)) {
            session.setLock(doc);
        }
        return session.getDocument(doc);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (canLock(doc)) {
            session.setLock(doc.getRef());
        }
        return session.getDocument(doc.getRef());
    }

    private boolean canLock(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        if (doc != null) {
            return canLock(doc);
        }
        return false;
    }

    private boolean canLock(DocumentModel doc) {
        // TODO PowerUsers?
        // TODO Hau gaizki dago. Lifecycle egoera guztietan isLockable jartzen
        // dugunean XPa matxakatuta
        // hori bakarrik begiratu ahalko da, oraingoz ezin da.
        try {
            if (!doc.isFolder()
                    && !EloraDocumentHelper.getIsCurrentStateLockable(doc)) {
                return false;
            }
        } catch (EloraException e) {
            return false;
        }

        return true;
    }

}
