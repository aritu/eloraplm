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
package com.aritu.eloraplm.cm.treetable;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Change Management tree beans.
 *
 * @author aritu
 *
 */
public class CMTreeBeanHelper {

    public static boolean calculateDestinationItemVersionListIsReadOnlyValue(
            String action, boolean isManaged) {

        boolean destinationItemVersionIsReadOnly = false;

        if (!(action.equals(CMConstants.ACTION_CHANGE)
                || action.equals(CMConstants.ACTION_REPLACE))
                || (action.equals(CMConstants.ACTION_CHANGE) && isManaged)
                || (action.equals(CMConstants.ACTION_REPLACE) && isManaged)) {
            destinationItemVersionIsReadOnly = true;
        }

        return destinationItemVersionIsReadOnly;
    }

    public static boolean calculateIsManagedIsReadOnlyValue(String action,
            DocumentModel destinationItem) {

        boolean isManagedIsReadOnly = false;
        /*
         * isManaged field is editable only if the action is not REMOVE or IGNORE
         * or if the destination item is not CHECKED OUT
         */
        if (action.equals(CMConstants.ACTION_REMOVE)
                || action.equals(CMConstants.ACTION_IGNORE)
                || (action.equals(CMConstants.ACTION_REPLACE)
                        && destinationItem == null)) {
            isManagedIsReadOnly = true;
        } else {
            if (destinationItem != null) {
                if (destinationItem.isCheckedOut()) {
                    isManagedIsReadOnly = true;
                }
            }
        }

        return isManagedIsReadOnly;
    }

    public static boolean calculatedImpactedItemActionIsReadOnlyValue(
            String action, String originItemWcUid, String originItemType,
            String modifiedItemAction, String modifiedItemDestinationWcUid,
            String parentAction) {
        boolean actionIsReadOnly = false;

        // if the action is Ignore, in some cases the action should not be
        // editable
        if (action.equals(CMConstants.ACTION_IGNORE)) {

            // if the action is ignored because the origin of the impacted item
            // is the same as the destination of the modified item, the action
            // must be readOnly
            if (modifiedItemAction.equals(CMConstants.ACTION_REPLACE)
                    && originItemWcUid.equals(modifiedItemDestinationWcUid)) {
                actionIsReadOnly = true;
            } else {
                if (parentAction != null
                        && parentAction.equals(CMConstants.ACTION_IGNORE)
                        && originItemType != null && !originItemType.equals(
                                EloraDoctypeConstants.CAD_DRAWING)) {
                    actionIsReadOnly = true;
                }
            }

        }

        return actionIsReadOnly;
    }

    public static void processRefreshNodeTriggeredByIsManaged(
            CMItemsNodeData nodeData, CoreSession session,
            Map<String, String> messages) throws EloraException {

        boolean isManaged = nodeData.getIsManaged();

        String action = nodeData.getAction();
        if (action != null && (action.equals(CMConstants.ACTION_CHANGE)
                || action.equals(CMConstants.ACTION_REPLACE))) {
            DocumentModel destinationItem = nodeData.getDestinationItem();

            // if isManaged = true, change destinationItem to the AV
            if (isManaged) {
                if (destinationItem != null && !destinationItem.isVersion()) {
                    DocumentModel newDestinationItem = EloraDocumentHelper.getLatestVersion(
                            destinationItem);
                    nodeData.setDestinationItem(newDestinationItem);
                    nodeData.setDestinationItemVersionIsReadOnly(true);
                }
            } else {
                // put back again the WC and display the list
                nodeData.setDestinationItem(nodeData.getDestinationItemWc());

                if (nodeData.getDestinationItemVersionList() == null
                        || nodeData.getDestinationItemVersionList().size() == 0) {
                    loadDestinationVersions(nodeData, session, messages);
                }

                nodeData.setDestinationItemUid(
                        nodeData.getDestinationItemWc().getId());

                nodeData.setDestinationItemVersionIsReadOnly(false);

                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                        nodeData.getAction(), nodeData.getDestinationItem());
                nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
            }
        }
    }

    public static void processRefreshNodeTriggeredByDestinationItemUid(
            CMItemsNodeData nodeData, CoreSession session)
            throws EloraException {

        String destinationItemUid = nodeData.getDestinationItemUid();

        DocumentModel destinationItem = session.getDocument(
                new IdRef(destinationItemUid));

        nodeData.setDestinationItem(destinationItem);

        if (destinationItem.isVersion()) {
            nodeData.setIsManaged(true);
            nodeData.setDestinationItemVersionIsReadOnly(true);
        } else {
            nodeData.setIsManaged(false);
            nodeData.setDestinationItemVersionIsReadOnly(false);
        }

        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                nodeData.getAction(), nodeData.getDestinationItem());
        nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);

    }

    public static void loadDestinationVersions(CMItemsNodeData nodeData,
            CoreSession session, Map<String, String> messages)
            throws EloraException {

        Map<String, String> versionList = new HashMap<String, String>();

        DocumentModel destinationItemWc = nodeData.getDestinationItemWc();
        if (destinationItemWc != null) {
            String destinationItemWcUid = destinationItemWc.getId();

            // calculate the version list
            versionList = CMHelper.calculateModifiableItemVersionList(session,
                    destinationItemWcUid, messages);

            // Add also the WC at the end
            versionList.put(destinationItemWcUid,
                    destinationItemWc.getVersionLabel() + " (WC)");
        }

        nodeData.setDestinationItemVersionList(versionList);
    }

    // -----------------------------------------------------------------------------
    // The way to calculate the comment of the modified and impacted items have
    // been changed. We keep the code commented, in case we need to change it
    // again.
    /*private static String getModifiedItemActionLabel(String action)
            throws EloraException {
    
        String actionLabel = null;
    
        if (!CMConfig.modifiedActionsLabelMap.containsKey(action)) {
            throw new EloraException("label for key = |" + action
                    + "| in ModifiedActionsConfig is null");
        }
        actionLabel = CMConfig.modifiedActionsLabelMap.get(action);
    
        return actionLabel;
    }
    
    public static String calculateModifiedItemComment(String action,
            DocumentModel cmProcess, DocumentModel originItem,
            DocumentModel destinationItem) throws EloraException {
    
        String actionLabel = getModifiedItemActionLabel(action);
    
        String comment = "["
                + cmProcess.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                + "] " + actionLabel + ": ";
    
        switch (action) {
        case CMConstants.ACTION_CHANGE:
            comment += cmProcess.getTitle();
            break;
        case CMConstants.ACTION_REMOVE:
            comment += originItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE) + " "
                    + originItem.getTitle();
            break;
    
        case CMConstants.ACTION_REPLACE:
            comment += originItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE) + " "
                    + originItem.getTitle() + " by "
                    + destinationItem.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE)
                    + " " + destinationItem.getTitle();
            break;
        case CMConstants.ACTION_IGNORE:
            // empty comment
            comment = "";
            break;
        }
    
        return comment;
    }

    public static String calculateImpactedItemComment(DocumentModel cmProcess,
            String modifiedItemAction, DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem) throws EloraException {

        String actionLabel = getModifiedItemActionLabel(modifiedItemAction);

        String comment = "["
                + cmProcess.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                + "] " + actionLabel + ": ";

        switch (modifiedItemAction) {
        case CMConstants.ACTION_CHANGE:
        case CMConstants.ACTION_REMOVE:
            comment += modifiedItemOriginItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE) + " "
                    + modifiedItemOriginItem.getTitle();
            break;

        case CMConstants.ACTION_REPLACE:
            comment += modifiedItemOriginItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE) + " "
                    + modifiedItemOriginItem.getTitle() + " by "
                    + modifiedItemDestinationItem.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE)
                    + " " + modifiedItemDestinationItem.getTitle();
            break;
        }

        return comment;
    }*/
    // -----------------------------------------------------------------------------

    public static String calculateComment(DocumentModel cmProcess,
            String action) throws EloraException {

        String comment = null;

        if (!action.equals(CMConstants.ACTION_IGNORE)) {
            comment = "["
                    + cmProcess.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE)
                    + "] " + cmProcess.getTitle();
        }
        return comment;
    }

}
