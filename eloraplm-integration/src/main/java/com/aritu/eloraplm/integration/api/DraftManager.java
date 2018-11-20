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
package com.aritu.eloraplm.integration.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface DraftManager {

    /**
     * @param doc
     * @param structureRootRealRef
     * @return
     * @throws EloraException
     */
    DocumentModel createDraftForDocument(DocumentModel doc,
            DocumentRef structureRootRealRef) throws EloraException;

    /**
     * @param session
     * @param structureRootRealRef
     * @return
     */
    String getTempFolderPath(CoreSession session,
            DocumentRef structureRootRealRef) throws EloraException;

    /**
     * @param session
     * @param wcDoc
     * @param username
     * @param required
     * @return
     * @throws EloraException
     */
    DocumentModel getDraftForDocument(CoreSession session, DocumentModel wcDoc,
            String username, boolean required) throws EloraException;

    /**
     * Remove the draft related to the document and created by the current user
     *
     * @param session
     * @param wcDocRef
     * @throws EloraException
     */
    void removeDocumentDraft(CoreSession session, DocumentRef wcDocRef)
            throws EloraException;

    /**
     * @param session
     * @param destinationDoc
     */
    void copyDraftDataAndRemoveIt(CoreSession session,
            DocumentModel destinationDoc) throws EloraException;

    /**
     * @return
     */
    Map<String, List<String>> getExcludedPropertiesFromCopy();

}
