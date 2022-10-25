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
package com.aritu.eloraplm.pdm.makeobsolete.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;

/**
 * Service interface for make obsolete function.
 *
 * @author aritu
 *
 */
public interface MakeObsoleteService {

    public CanMakeObsoleteResult canMakeObsoleteDocument(CoreSession session,
            DocumentModel doc) throws EloraException;

    /**
     * @param session
     * @param doc
     * @return if it is not possible to make obsolete the specified document, it
     *         returns the reason.
     * @throws EloraException
     */
    public CanMakeObsoleteResult makeObsoleteDocument(CoreSession session,
            DocumentModel doc) throws EloraException;

    /**
     * This method makes obsolete the specified documents list. First of all, it
     * checks if it is possible to make them obsolete.
     *
     * @param session current session.
     * @param docs documents list to make obsolete.
     * @return if it is not possible to make obsolete the specified documents
     *         list, it returns the reason.
     * @throws EloraException
     */
    public Map<String, CanMakeObsoleteResult> makeObsoleteDocumentList(
            CoreSession session, List<DocumentModel> docs)
            throws EloraException;

    /**
     * @param session
     * @param doc
     * @return
     * @throws EloraException
     */
    public CanMakeObsoleteResult canMakeObsoleteDocumentWithoutRelationChecks(
            CoreSession session, DocumentModel doc) throws EloraException;

    /**
     * @param session
     * @param doc
     * @param processReference
     * @return
     * @throws EloraException
     */
    public CanMakeObsoleteResult makeObsoleteDocumentInProcess(
            CoreSession session, DocumentModel doc, String processReference)
            throws EloraException;

}
