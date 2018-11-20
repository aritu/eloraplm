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
import org.nuxeo.ecm.core.api.DocumentModelList;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.CMDocTypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.history.api.VersionRemoveCheckerManager;

public class VersionRemoveCheckerService
        implements VersionRemoveCheckerManager {

    private static final Log log = LogFactory.getLog(
            VersionRemoveCheckerService.class);

    @Override
    public boolean canRemoveDocument(DocumentModel doc, DocumentModel wcDoc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        if (isRemovableVersion(doc, wcDoc, facesMessages, messages, session)
                && !hasParentsThatAreNotAnarchic(doc, facesMessages, messages,
                        session)
                && !isUsedInEco(doc, facesMessages, messages, session)) {
            return true;
        } else {
            return false;
        }
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

    protected boolean isUsedInEco(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        if (!isUsedInEcoAsOrigin(doc, facesMessages, messages, session)
                && !isUsedInEcoAsDestination(doc, facesMessages, messages,
                        session)
                && !isUsedInImpactMatrixAsOrigin(doc, facesMessages, messages,
                        session)
                && !isUsedInImpactMatrixAsDestination(doc, facesMessages,
                        messages, session)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isUsedInEcoAsOrigin(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        String logInitMsg = "[isUsedInEcoAsOrigin] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByModifiedItemOriginQuery(
                CMDocTypeConstants.CM_ECO, doc.getId(),
                CMHelper.getItemType(doc));
        DocumentModelList ecoOriginList = session.query(query);
        if (ecoOriginList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoOriginDoc : ecoOriginList) {
                docListMsg = docListMsg + ", "
                        + ecoOriginDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoOriginDoc.getTitle();
            }
            log.error(logInitMsg + "Version is used as origin in ECOs: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.removeVersion.isUsedInEcoAsOrigin"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedInEcoAsDestination(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        String logInitMsg = "[isUsedInEcoAsDestination] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByModifiedItemDestinationQuery(
                CMDocTypeConstants.CM_ECO, doc.getId(),
                CMHelper.getItemType(doc));
        DocumentModelList ecoDestinationList = session.query(query);
        if (ecoDestinationList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestDoc : ecoDestinationList) {
                docListMsg = docListMsg + ", "
                        + ecoDestDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestDoc.getTitle();
            }
            log.error(logInitMsg + "Version is used as destination in ECOs: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.removeVersion.isUsedInEcoAsDestination"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedInImpactMatrixAsOrigin(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        String logInitMsg = "[isUsedInImpactMatrixAsOrigin] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByImpactedItemOriginQuery(
                CMDocTypeConstants.CM_ECO, doc.getId(),
                CMHelper.getItemType(doc));
        DocumentModelList ecoDestImpactedList = session.query(query);
        if (ecoDestImpactedList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestImpactedDoc : ecoDestImpactedList) {
                docListMsg = docListMsg + ", "
                        + ecoDestImpactedDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestImpactedDoc.getTitle();
            }
            log.error(
                    logInitMsg + "Version is used in matrix as origin in ECOs: "
                            + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.removeVersion.isUsedInMatrixAsOrigin"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
        }
    }

    private boolean isUsedInImpactMatrixAsDestination(DocumentModel doc,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) {
        String logInitMsg = "[isUsedInImpactMatrixAsDestination] ["
                + session.getPrincipal().getName() + "] ";
        String query = CMQueryFactory.getProcessDocumentModelsByImpactedItemDestinationQuery(
                CMDocTypeConstants.CM_ECO, doc.getId(),
                CMHelper.getItemType(doc));
        DocumentModelList ecoDestImpactedList = session.query(query);
        if (ecoDestImpactedList.size() > 0) {
            String docListMsg = "";
            for (DocumentModel ecoDestImpactedDoc : ecoDestImpactedList) {
                docListMsg = docListMsg + ", "
                        + ecoDestImpactedDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + " - " + ecoDestImpactedDoc.getTitle();
            }
            log.error(logInitMsg
                    + "Version is used in matrix as destination in ECOs: "
                    + docListMsg.substring(2));
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.removeVersion.isUsedInMatrixAsDestination"),
                    docListMsg.substring(2));
            return true;
        } else {
            return false;
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
