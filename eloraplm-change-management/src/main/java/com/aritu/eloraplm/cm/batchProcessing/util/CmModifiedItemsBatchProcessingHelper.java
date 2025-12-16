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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.treetable.CMItemsNodeData;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Change Management Batch Processing.
 *
 * @author aritu
 *
 */
public class CmModifiedItemsBatchProcessingHelper {

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsBatchProcessingHelper.class);

    public static List<CMItemsNodeData> checkAndAddTreeDocumentsForPromote(
            TreeNode root, CoreSession documentManager) throws EloraException {

        List<CMItemsNodeData> nodeDataListToProcess = new ArrayList<CMItemsNodeData>();

        String logInitMsg = "[checkAndAddTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {
            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();
            if (canNodeDataBeAddedToPromote(childNodeData)) {
                nodeDataListToProcess.add(childNodeData);
            }
            for (TreeNode derivedNode : childNode.getChildren()) {
                CMItemsNodeData derivedNodeData = (CMItemsNodeData) derivedNode.getData();
                if (canNodeDataBeAddedToPromote(derivedNodeData)) {
                    nodeDataListToProcess.add(derivedNodeData);
                }
            }
        }
        log.trace(logInitMsg + "|" + nodeDataListToProcess.size()
                + "| documents for checkout.");

        return nodeDataListToProcess;
    }

    private static boolean canNodeDataBeAddedToPromote(CMItemsNodeData nodeData)
            throws EloraException {

        if (CmBatchProcessingHelper.isManaged(nodeData)
                && !CmBatchProcessingHelper.isIgnored(nodeData)
                && !CmBatchProcessingHelper.isRemoved(nodeData)) {

            DocumentModel destItem = nodeData.getDestinationItem();
            if (!EloraDocumentHelper.isReleased(destItem)) {
                return true;
            }
        }
        return false;
    }

}
