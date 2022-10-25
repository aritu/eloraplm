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

    public static int checkAndcountTreeDocumentsForPromote(TreeNode root,
            CoreSession documentManager) throws EloraException {
        int totalDocs = 0;

        String logInitMsg = "[checkAndcountTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {

            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isRemoved(childNodeData)) {

                DocumentModel destItem = childNodeData.getDestinationItem();
                if (!EloraDocumentHelper.isReleased(destItem)) {
                    totalDocs++;
                }
            }
        }
        log.trace(logInitMsg + "|" + totalDocs + "| documents for promote.");

        return totalDocs;

    }

}
