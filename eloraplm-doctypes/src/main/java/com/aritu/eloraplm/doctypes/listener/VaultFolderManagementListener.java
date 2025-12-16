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

package com.aritu.eloraplm.doctypes.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraPermissionHelper;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
public class VaultFolderManagementListener implements EventListener {

    protected static Log log = LogFactory.getLog(
            VaultFolderManagementListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                if (!doc.hasFacet(EloraFacetConstants.FACET_VAULT_FOLDER)
                        && !isDocTypeHandled(doc.getType())) {
                    return;
                }

                String eventName = event.getName();
                log.trace(logInitMsg + "doc id = |" + doc.getId()
                        + "|, doctype = |" + doc.getType() + "|, eventName = |"
                        + eventName + "|");

                CoreSession session = docEventContext.getCoreSession();

                try {
                    switch (eventName) {
                    case DocumentEventTypes.DOCUMENT_CREATED:
                        if (doc.getType().equals(
                                EloraDoctypeConstants.OTHER_DOC_VAULT_FOLDER)) {
                            handleOtherDocVaultFolderCreation(doc);
                        }
                        break;

                    case DocumentEventTypes.DOCUMENT_REMOVED:
                        if (doc.getType().equals(
                                EloraDoctypeConstants.OTHER_DOC_VAULT_FOLDER)) {
                            handleOtherDocVaultFolderDeletion(doc.getId());
                        }
                        break;

                    case DocumentEventTypes.DOCUMENT_UPDATED:
                        if (doc.hasFacet(
                                EloraFacetConstants.FACET_VAULT_FOLDER)) {
                            if (doc.getType().equals(
                                    EloraDoctypeConstants.OTHER_DOC_VAULT_FOLDER)) {
                                handleOtherDocVaultFolderModification(doc);
                            }
                        } else {
                            handleEloraRootFolderModification(doc);
                        }
                        break;
                    }
                } catch (Exception e) {

                    // TODO LEIRE: erroreren bat baldin badago, hobeto
                    // kudeatu...

                    log.error(logInitMsg + e.getMessage(), e);
                    // Rollback the event
                    event.markRollBack(e.getMessage(), e);
                    throw e;
                }

                session.save();

            }
        }
    }

    protected boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledEventsName() {
        return Arrays.asList(DocumentEventTypes.DOCUMENT_CREATED,
                DocumentEventTypes.DOCUMENT_REMOVED,
                DocumentEventTypes.DOCUMENT_UPDATED);
    }

    protected boolean isDocTypeHandled(String docType) {
        for (String handledDocType : getHandledDocTypes()) {
            if (handledDocType.equals(docType)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledDocTypes() {
        return Arrays.asList(
                new String[] { EloraDoctypeConstants.STRUCTURE_ROOT,
                        EloraDoctypeConstants.COLLABORATION_ROOT,
                        EloraDoctypeConstants.LIBRARY_ROOT });
    }

    private void handleOtherDocVaultFolderCreation(
            DocumentModel otherDocVaultFolder) {

        new UnrestrictedSessionRunner("default") {
            @Override
            public void run() {

                // Create OtherDoc Vault Folder related groups
                UserManager userManager = Framework.getService(
                        UserManager.class);
                // Retrieve vault folder document from unrestricted session
                DocumentModel unrestrictedOtherDocVaultFolder = session.getDocument(
                        otherDocVaultFolder.getRef());
                List<String> createdGroupLabels = EloraPermissionHelper.createVaultFolderGroupsAndSetPermission(
                        userManager, unrestrictedOtherDocVaultFolder);

                // Add info message to facesMessages
                FacesMessages facesMessages = (FacesMessages) Contexts.getConversationContext().get(
                        FacesMessages.class);
                facesMessages.add(StatusMessage.Severity.INFO,
                        Messages.instance().get(
                                "eloraplm.message.info.vaultfolder.groupsCreated.msg1"));
            }
        }.runUnrestricted();

    }

    private void handleOtherDocVaultFolderDeletion(
            String otherDocVaultFolderId) {

        new UnrestrictedSessionRunner("default") {
            @Override
            public void run() {

                // Delete Vault Folder related groups
                UserManager userManager = Framework.getService(
                        UserManager.class);
                EloraPermissionHelper.deleteVaultFolderGroups(userManager,
                        otherDocVaultFolderId);

            }
        }.runUnrestricted();
    }

    private void handleOtherDocVaultFolderModification(
            DocumentModel otherDocVaultFolder) {

        new UnrestrictedSessionRunner("default") {
            @Override
            public void run() {

                // Update Vault Folder related groups
                UserManager userManager = Framework.getService(
                        UserManager.class);
                // Retrieve vault folder document from unrestricted session
                DocumentModel unrestrictedOtherDocVaultFolder = session.getDocument(
                        otherDocVaultFolder.getRef());
                EloraPermissionHelper.updateVaultFolderGroups(userManager,
                        unrestrictedOtherDocVaultFolder);

            }
        }.runUnrestricted();
    }

    private void handleEloraRootFolderModification(
            DocumentModel eloraRootFolder) {

        new UnrestrictedSessionRunner("default") {
            @Override
            public void run() {

                // Get all vault folders located under this eloraRootFolder
                String vaultFolderQuery = EloraQueryFactory.getOtherDocumentsVaultFoldersInsideAncestorQuery(
                        eloraRootFolder.getId(),
                        NuxeoMetadataConstants.NX_DC_TITLE);

                DocumentModelList otherDocVaultFolders = session.query(
                        vaultFolderQuery);

                if (otherDocVaultFolders != null
                        && otherDocVaultFolders.size() > 0) {
                    UserManager userManager = Framework.getService(
                            UserManager.class);

                    // Update Vault Folder related groups
                    for (DocumentModel otherDocVaultFolder : otherDocVaultFolders) {
                        EloraPermissionHelper.updateVaultFolderGroups(
                                userManager, otherDocVaultFolder,
                                eloraRootFolder.getTitle());
                    }
                }
            }
        }.runUnrestricted();
    }
}
