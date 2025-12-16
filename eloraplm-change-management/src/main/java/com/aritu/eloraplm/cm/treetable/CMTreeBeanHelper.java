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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

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
                || action.equals(CMConstants.ACTION_OVERWRITE)
                || action.equals(CMConstants.ACTION_REPLACE))
                || ((action.equals(CMConstants.ACTION_CHANGE)
                        || action.equals(CMConstants.ACTION_OVERWRITE)
                        || action.equals(CMConstants.ACTION_REPLACE))
                        && isManaged)) {
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
                || action.equals(CMConstants.ACTION_IGNORE)) {
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
            String action, String originItemWcUid, boolean isFromMbom,
            String originItemType, String modifiedItemAction,
            String modifiedItemDestinationWcUid, String parentAction) {
        boolean actionIsReadOnly = false;

        if (originItemWcUid == null && isFromMbom) {
            actionIsReadOnly = true;
        } else if (action.equals(CMConstants.ACTION_IGNORE)) {

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

    public static boolean calculateDestinationItemSourceVersionIsReadOnlyValue(
            DocumentModel destinationItem, boolean isManaged) {

        boolean destinationItemSourceVersionIsReadOnly = false;

        if (isManaged || destinationItem.isCheckedOut()) {
            destinationItemSourceVersionIsReadOnly = true;
        }

        return destinationItemSourceVersionIsReadOnly;
    }

    public static void processRefreshNodeTriggeredByIsManaged(
            CMItemsNodeData nodeData, CoreSession session,
            Map<String, String> messages) throws EloraException {

        boolean isManaged = nodeData.getIsManaged();

        String action = nodeData.getAction();
        if (action != null && (action.equals(CMConstants.ACTION_CHANGE)
                || action.equals(CMConstants.ACTION_OVERWRITE)
                || action.equals(CMConstants.ACTION_REPLACE))) {
            DocumentModel destinationItem = nodeData.getDestinationItem();

            // if isManaged = true, change destinationItem to the AV
            if (isManaged) {
                if (destinationItem != null && !destinationItem.isVersion()) {
                    DocumentModel newDestinationItem = EloraDocumentHelper.getLatestVersion(
                            destinationItem);
                    nodeData.setDestinationItem(newDestinationItem);
                    nodeData.setDestinationItemVersionIsReadOnly(true);
                    nodeData.setDestinationItemSourceVersionIsReadOnly(true);
                }
            } else {
                // put back again the WC and display the list
                nodeData.setDestinationItem(nodeData.getDestinationItemWc());
                if (nodeData.getDestinationItemVersionList() == null
                        || nodeData.getDestinationItemVersionList().size() == 0) {
                    loadDestinationVersions(nodeData, session, messages);
                }
                nodeData.setDestinationItemVersionIsReadOnly(false);
                boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                        .calculateDestinationItemSourceVersionIsReadOnlyValue(
                                nodeData.getDestinationItemWc(), isManaged);
                nodeData.setDestinationItemSourceVersionIsReadOnly(
                        destinationItemSourceVersionIsReadOnly);
                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                        nodeData.getAction(), nodeData.getDestinationItem());
                nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
            }
        }
    }

    public static void processRefreshNodeTriggeredByDestinationItem(
            CMItemsNodeData nodeData, CoreSession session)
            throws EloraException {

        DocumentModel destinationItem = nodeData.getDestinationItem();

        nodeData.setDestinationItem(destinationItem);

        if (destinationItem.isVersion()) {
            nodeData.setIsManaged(true);
            nodeData.setDestinationItemVersionIsReadOnly(true);
            nodeData.setDestinationItemSourceVersionIsReadOnly(true);
        } else {
            boolean isManaged = false;
            nodeData.setIsManaged(isManaged);
            nodeData.setDestinationItemVersionIsReadOnly(false);
            boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                    .calculateDestinationItemSourceVersionIsReadOnlyValue(
                            destinationItem, isManaged);
            nodeData.setDestinationItemSourceVersionIsReadOnly(
                    destinationItemSourceVersionIsReadOnly);
        }

        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                nodeData.getAction(), nodeData.getDestinationItem());
        nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
    }

    public static void processRefreshNodeTriggeredByDestinationItemSourceVersion(
            CMItemsNodeData nodeData, CoreSession session)
            throws EloraException {

        DocumentModel destinationItemSourceVersion = nodeData.getDestinationItemSourceVersion();

        nodeData.setDestinationItemSourceVersion(destinationItemSourceVersion);
    }

    public static void loadDestinationVersions(CMItemsNodeData nodeData,
            CoreSession session, Map<String, String> messages)
            throws EloraException {

        List<SelectItem> versionList = new ArrayList<SelectItem>();

        DocumentModel destinationItemWc = nodeData.getDestinationItemWc();
        if (destinationItemWc != null) {
            String destinationItemWcUid = destinationItemWc.getId();

            // calculate the version list
            versionList = CMHelper.calculateModifiableItemVersionList(session,
                    destinationItemWcUid, messages);

            // Add also the WC at the beginning
            versionList.add(0, new SelectItem(destinationItemWc,
                    destinationItemWc.getVersionLabel()
                            + CMConstants.VERSION_LABEL_WC));
        }

        nodeData.setDestinationItemVersionList(versionList);
    }

    public static void loadDestinationSourceVersions(CMItemsNodeData nodeData,
            CoreSession session, Map<String, String> messages)
            throws EloraException {

        List<SelectItem> sourceVersionList = new ArrayList<SelectItem>();

        DocumentModel destinationItemWc = nodeData.getDestinationItemWc();
        if (destinationItemWc != null) {
            String destinationItemWcUid = destinationItemWc.getId();

            // calculate the source version list
            sourceVersionList = CMHelper.calculateDestinationItemSourceVersionList(
                    session, destinationItemWcUid, messages);
        }

        nodeData.setDestinationItemSourceVersionList(sourceVersionList);
    }

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
