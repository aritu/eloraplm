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
package com.aritu.eloraplm.history.services;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_VERSION;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.history.api.VersionRemoveCheckerService;

public class VersionRemoveCheckerServiceImpl
        implements VersionRemoveCheckerService {

    private static final Log log = LogFactory.getLog(
            VersionRemoveCheckerServiceImpl.class);

    @Override
    public boolean canRemoveDocument(DocumentModel doc, DocumentModel wcDoc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {

        boolean canRemoveDocument = false;
        if (isRemovableVersion(doc, wcDoc, facesMessages, messages, session)
                && !hasParentsThatAreNotAnarchic(doc, facesMessages, messages,
                        session)) {
            canRemoveDocument = true;
        }
        return canRemoveDocument;
    }

    protected boolean hasParentsThatAreNotAnarchic(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        try {
            EloraRelationHelper.checkForNoAnarchicParents(doc);
            return false;
        } catch (EloraException e) {
            String logInitMsg = "[hasParentsThatAreNotAnarchic] ["
                    + session.getPrincipal().getName() + "] ";
            log.error(logInitMsg + "Version has not anarchic parents");
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.hasParents"));
            return true;
        }
    }

    protected boolean isRemovableVersion(DocumentModel doc, DocumentModel wcDoc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        List<DocumentModel> proxies = session.getProxies(doc.getRef(), null);
        if (!proxies.isEmpty()) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.hasProxies"));
            return false;
        }
        if (session.hasPermission(wcDoc.getRef(), WRITE_VERSION)) {
            return true;
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.noRightsToRemoveVersion"));
            return false;
        }
    }

}
