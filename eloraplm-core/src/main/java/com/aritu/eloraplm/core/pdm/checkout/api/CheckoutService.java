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
package com.aritu.eloraplm.core.pdm.checkout.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;

import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Checkout service interface.
 *
 * @author aritu
 *
 */

public interface CheckoutService {

    DocumentModel checkOut(DocumentModel doc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException;

    DocumentModel checkOutFromVersion(DocumentModel doc,
            VersionModel versionFrom)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException;

    DocumentModel checkOutInCmProcess(DocumentModel doc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException;

    DocumentModel checkOutFromVersionInCmProcess(DocumentModel doc,
            DocumentModel versionFromDoc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException;

    DocumentModel undoCheckout(DocumentModel doc) throws EloraException;

}
