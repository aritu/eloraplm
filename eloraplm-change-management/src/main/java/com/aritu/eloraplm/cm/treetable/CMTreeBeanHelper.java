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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CMTreeBeanHelper {

    private static final Log log = LogFactory.getLog(CMTreeBeanHelper.class);

    /**
     *
     */
    public CMTreeBeanHelper() {
        // TODO Auto-generated constructor stub
    }

    public static boolean calculateIsManagedIsReadOnlyValue(String action,
            DocumentModel destinationItem,
            EloraConfigTable releasedStatesConfig) {

        boolean isManagedIsReadOnly = false;

        if (action.equals(CMConstants.ACTION_REMOVE)
                || action.equals(CMConstants.ACTION_IGNORE)) {
            isManagedIsReadOnly = true;
        } else {
            // isManaged field is editable only if the destination item is in a
            // released state
            if (destinationItem != null) {
                String desinationItemState = destinationItem.getCurrentLifeCycleState();
                if (!releasedStatesConfig.containsKey(desinationItemState)) {
                    isManagedIsReadOnly = true;
                }
            }
        }

        return isManagedIsReadOnly;
    }

    public static boolean calculatedImpactedItemActionIsReadOnlyValue(
            String action, TreeNode parentNode) {
        boolean actionIsReadOnly = false;

        // if the action is Ignore, destination item is not editable
        if (action.equals(CMConstants.ACTION_IGNORE)) {
            // If the parentNode Action is also Ignore, this item
            // action must be readOnly.
            String parentAction = "";
            String parentNodeClassName = parentNode.getData().getClass().getSimpleName();
            if (parentNodeClassName.equals(
                    CmModifiedItemsNodeData.class.getSimpleName())) {
                CmModifiedItemsNodeData parentNodeData = (CmModifiedItemsNodeData) parentNode.getData();
                parentAction = parentNodeData.getAction();
            }
            if (parentNodeClassName.equals(
                    CmImpactedItemsNodeData.class.getSimpleName())) {
                CmImpactedItemsNodeData parentNodeData = (CmImpactedItemsNodeData) parentNode.getData();
                parentAction = parentNodeData.getAction();
            }
            if (parentAction.equals(CMConstants.ACTION_IGNORE)) {
                actionIsReadOnly = true;
            }
        }

        return actionIsReadOnly;
    }

    public static void processRefreshNodeTriggeredByIsManaged(
            CmItemsNodeData nodeData, CoreSession session)
            throws EloraException {

        boolean isManaged = nodeData.getIsManaged();

        String action = nodeData.getAction();
        if (action != null && action.equals(CMConstants.ACTION_CHANGE)) {
            DocumentModel destinationItem = nodeData.getDestinationItem();

            // if isManaged = true, change destinationItem to the AV
            if (isManaged) {
                if (destinationItem != null && !destinationItem.isVersion()) {
                    DocumentModel newDestinationItem = EloraDocumentHelper.getLatestVersion(
                            destinationItem, session);
                    nodeData.setDestinationItem(newDestinationItem);
                }
            } else {
                // put back again the WC
                if (destinationItem != null && destinationItem.isVersion()) {
                    DocumentModel newDestinationItem = session.getWorkingCopy(
                            destinationItem.getRef());
                    nodeData.setDestinationItem(newDestinationItem);
                }
            }
        }

    }
}
