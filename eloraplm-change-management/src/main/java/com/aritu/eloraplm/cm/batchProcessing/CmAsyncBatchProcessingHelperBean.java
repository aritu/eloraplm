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
package com.aritu.eloraplm.cm.batchProcessing;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMBatchProcessingMetadataConstants;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;

@AutoCreate
@Name("cmAsyncBatchProcessingHelper")
public class CmAsyncBatchProcessingHelperBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmAsyncBatchProcessingHelperBean.class);

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    /**
     * Invalidates current document in the navigation context. It launches an
     * event to refresh also the impacted tree ben of the specified type (DOC,
     * BOM)
     *
     * @param itemType
     */
    public void refreshDocument(String itemType) {
        String logInitMsg = "[refreshDocument] ["
                + documentManager.getPrincipal().getName() + "] "
                + " itemType = |" + itemType + "| ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        currentDoc.setPropertyValue(
                CMBatchProcessingMetadataConstants.NEED_TO_BE_REFRESHED, false);

        // Launch event indicating that process has been refreshed
        Events.instance().raiseEvent(CMBatchProcessingEventNames.REFRESHED,
                currentDoc.getId());

        documentManager.saveDocument(currentDoc);

        // Invalidate current document
        navigationContext.invalidateCurrentDocument();

        // refresh required impact matrix in function of the itemType:
        // ITEM or DOC
        // if itemType is null or empty, nothing to do. It means that this
        // process is not stored in the bean. This can happen after
        // reinitializing the server.
        if (itemType != null && itemType.length() > 0) {
            try {
                switch (itemType) {
                case CMConstants.ITEM_TYPE_BOM:
                    Events.instance().raiseEvent(
                            CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX);
                    break;
                case CMConstants.ITEM_TYPE_DOC:
                    Events.instance().raiseEvent(
                            CMEventNames.CM_REFRESH_DOCS_IMPACT_MATRIX);
                    break;

                default:
                    log.error(logInitMsg + "Invalid item type |" + itemType
                            + "|.");
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.cm.batch.refreshDocument"));
                }
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public String getActionLabel(String action) {

        String actionLabel = "";

        if (action != null && action.length() > 0) {
            actionLabel = messages.get("eloraplm.command.cm.batch." + action);

            if (actionLabel == null || actionLabel.length() == 0) {
                actionLabel = action;
            }
        }

        return actionLabel;

    }

}