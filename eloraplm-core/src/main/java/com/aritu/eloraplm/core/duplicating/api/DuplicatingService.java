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
package com.aritu.eloraplm.core.duplicating.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface DuplicatingService {

    public boolean canDocBeDuplicated(CoreSession session, DocumentModel doc)
            throws EloraException;

    /**
     * Duplicate a document in a single step, changing its reference and title
     *
     * @param session
     * @param sourceDoc
     * @return
     * @throws EloraException
     */
    public DocumentModel duplicate(CoreSession session, DocumentModel sourceDoc,
            String reference, String title) throws EloraException;

    /**
     * Duplicate a document's metadata (to use in a duplication performed step
     * by step)
     *
     * @param session
     * @param sourceDoc
     * @param reference
     * @param title
     * @return
     * @throws EloraException
     */
    public DocumentModel prepareDuplicatedDocument(CoreSession session,
            DocumentModel sourceDoc) throws EloraException;

    /**
     * Create the duplicated document, its relations and content (if it is
     * folderish) (to use in a duplication performed step by step)
     *
     * @param session
     * @param sourceDoc
     * @param duplicatedDoc
     * @throws EloraException
     */
    public DocumentModel completeDuplication(CoreSession session,
            DocumentModel sourceDoc, DocumentModel duplicatedDoc)
            throws EloraException;

}
