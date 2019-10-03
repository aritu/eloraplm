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
package com.aritu.eloraplm.cm.history.services;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.history.api.VersionRemoveCheckerService;
import com.aritu.eloraplm.history.services.VersionRemoveCheckerServiceImpl;

public class CmVersionRemoveCheckerServiceImpl extends
        VersionRemoveCheckerServiceImpl implements VersionRemoveCheckerService {

    private static final Log log = LogFactory.getLog(
            CmVersionRemoveCheckerServiceImpl.class);

    @Override
    public boolean canRemoveDocument(DocumentModel doc, DocumentModel wcDoc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {

        boolean canRemoveDocument = false;
        try {
            if (super.canRemoveDocument(doc, wcDoc, facesMessages, messages,
                    session)
                    && !isUsedInCmProcess(doc, facesMessages, messages,
                            session)) {
                canRemoveDocument = true;
            }
        } catch (EloraException e) {
            String logInitMsg = "[canRemoveDocument] ["
                    + session.getPrincipal().getName() + "] ";
            log.error(logInitMsg
                    + "An exception occured checking if a document can be removed. Exception message is: "
                    + e.getMessage(), e);
        }
        return canRemoveDocument;
    }

    protected boolean isUsedInCmProcess(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {
        if (!isUsedAsModifiedOrigin(doc, facesMessages, messages, session)
                && !isUsedAsModifiedDestination(doc, facesMessages, messages,
                        session)
                && !isUsedAsImpactedOrigin(doc, facesMessages, messages,
                        session)
                && !isUsedAsImpactedDestination(doc, facesMessages, messages,
                        session)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isUsedAsModifiedOrigin(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {
        String logInitMsg = "[isUsedAsModifiedOrigin] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByModifiedItemOriginQuery(
                doc.getId(), CMHelper.getItemType(doc));
        DocumentModelList ecoOriginList = session.query(query);
        if (ecoOriginList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoOriginDoc : ecoOriginList) {
                docListMsg = docListMsg + ", "
                        + ecoOriginDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoOriginDoc.getTitle();
            }
            log.error(logInitMsg + "Version |" + doc.getId()
                    + "| is used  as as modified origin in following CM processes: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.isUsedAsModifiedOrigin"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedAsModifiedDestination(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {
        String logInitMsg = "[isUsedAsModifiedDestination] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByModifiedItemDestinationQuery(
                doc.getId(), CMHelper.getItemType(doc));
        DocumentModelList ecoDestinationList = session.query(query);
        if (ecoDestinationList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestDoc : ecoDestinationList) {
                docListMsg = docListMsg + ", "
                        + ecoDestDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestDoc.getTitle();
            }
            log.error(logInitMsg + "Version |" + doc.getId()
                    + "| is used as modified destination in following CM processes: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.isUsedAsModifiedDestination"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedAsImpactedOrigin(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {
        String logInitMsg = "[isUsedAsImpactedOrigin] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByImpactedItemOriginQuery(
                doc.getId(), CMHelper.getItemType(doc));
        DocumentModelList ecoDestImpactedList = session.query(query);
        if (ecoDestImpactedList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestImpactedDoc : ecoDestImpactedList) {
                docListMsg = docListMsg + ", "
                        + ecoDestImpactedDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestImpactedDoc.getTitle();
            }
            log.error(logInitMsg + "Version |" + doc.getId()
                    + "| is used as impacted origin in following CM processes: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.isUsedAsImpactedOrigin"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedAsImpactedDestination(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {
        String logInitMsg = "[isUsedAsImpactedDestination] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByImpactedItemDestinationQuery(
                doc.getId(), CMHelper.getItemType(doc));
        DocumentModelList ecoDestImpactedList = session.query(query);
        if (ecoDestImpactedList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestImpactedDoc : ecoDestImpactedList) {
                docListMsg = docListMsg + ", "
                        + ecoDestImpactedDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestImpactedDoc.getTitle();
            }
            log.error(logInitMsg + "Version |" + doc.getId()
                    + "| is used as impacted destination in following CM processes: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.removeVersion.isUsedAsImpactedDestination"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

}
