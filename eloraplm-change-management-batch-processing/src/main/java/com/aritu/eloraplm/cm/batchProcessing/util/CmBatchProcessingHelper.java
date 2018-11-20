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
package com.aritu.eloraplm.cm.batchProcessing.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Change Management Batch Processing.
 *
 * @author aritu
 *
 */
public class CmBatchProcessingHelper {

    private static final Log log = LogFactory.getLog(
            CmBatchProcessingHelper.class);

    public static void toggleLockProcessableDocs(TreeNode node, boolean lock,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {

        String logInitMsg = "[toogleLockProcessableDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (!isIgnored(nodeData)) {
                // if (!nodeData.getIsManaged()) {
                if (lock) {
                    try {
                        lockDestinationDocument(nodeData, session);
                    } catch (DocumentAlreadyLockedException e) {
                        facesMessages.add(StatusMessage.Severity.WARN,
                                messages.get("eloraplm.message.error.locked"),
                                e.getDocument().getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                e.getDocument().getTitle());
                    } catch (DocumentInUnlockableStateException e) {
                        facesMessages.add(StatusMessage.Severity.WARN,
                                messages.get(
                                        "eloraplm.message.error.not.lockable"),
                                e.getDocument().getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                e.getDocument().getTitle());
                    } catch (DocumentLockRightsException e) {
                        facesMessages.add(StatusMessage.Severity.WARN,
                                messages.get(
                                        "eloraplm.message.error.lock.rights"),
                                e.getDocument().getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                e.getDocument().getTitle());
                    }

                } else {
                    unlockDestinationDocument(nodeData, session);
                }
                // }
                toggleLockProcessableDocs(childNode, lock, facesMessages,
                        messages, session);
            }
        }
    }

    public static boolean isIgnored(ImpactedItemsNodeData nodeData) {
        String action = nodeData.getAction();
        return action.equals(CMConstants.ACTION_IGNORE) ? true : false;
    }

    private static void lockDestinationDocument(ImpactedItemsNodeData nodeData,
            CoreSession session)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {
        DocumentModel destinationItem = nodeData.getDestinationItem();
        EloraDocumentHelper.lockDocument(destinationItem);
    }

    private static void unlockDestinationDocument(
            ImpactedItemsNodeData nodeData, CoreSession session) {
        DocumentModel destinationItem = nodeData.getDestinationItem();
        unlockDocument(destinationItem, session);

    }

    private static void unlockDocument(DocumentModel destinationItem,
            CoreSession session) {
        DocumentModel destinationWcDoc = session.getWorkingCopy(
                destinationItem.getRef());
        if (!destinationWcDoc.isCheckedOut()
                && EloraDocumentHelper.isLockedByUserOrAdmin(destinationWcDoc,
                        session)) {
            destinationWcDoc.removeLock();
        }
    }

    public static boolean isManaged(ImpactedItemsNodeData nodeData) {
        return nodeData.getIsManaged();
    }

}
