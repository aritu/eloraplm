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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.EVENT;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.StateLog;
import com.aritu.eloraplm.core.util.StateLogHelper;

/**
 * This class defines the available actions to display StateLog history from
 * front end.
 *
 * @author aritu
 *
 */

@Name("stateLogActions")
@Scope(EVENT)
public class StateLogActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StateLogActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected ArrayList<String> logStates = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public ArrayList<StateLogDisplay> retrieveDocumentStateLogList(
            DocumentModel document) {
        String logInitMsg = "[retrieveDocumentStateLogList] ["
                + documentManager.getPrincipal().getName() + "] ";

        ArrayList<StateLogDisplay> stateLogDisplayList = new ArrayList<StateLogDisplay>();

        ArrayList<HashMap<String, Object>> statesLogsList = new ArrayList<HashMap<String, Object>>();

        try {

            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            if (!currentDocument.hasFacet(
                    EloraFacetConstants.FACET_STORE_STATES_LOG)) {
                log.error(logInitMsg + "This document has not |"
                        + EloraFacetConstants.FACET_STORE_STATES_LOG
                        + "|facet. doc id = |" + currentDocument.getId() + "|");
                return stateLogDisplayList;
            }

            if (currentDocument.getPropertyValue(
                    EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST) != null) {
                statesLogsList.addAll(
                        (ArrayList<HashMap<String, Object>>) currentDocument.getPropertyValue(
                                EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST));
            }

            for (HashMap<String, Object> stateLogType : statesLogsList) {

                StateLog stateLog = StateLogHelper.createStateLog(stateLogType);

                String versionLabel = null;
                String checkinComment = null;

                String versionDocId = stateLog.getVersionDocId();
                if (versionDocId != null && versionDocId.length() > 0) {
                    try {
                        DocumentModel versionDM = documentManager.getDocument(
                                new IdRef(versionDocId));
                        if (versionDM != null) {
                            versionLabel = versionDM.getVersionLabel();
                            checkinComment = versionDM.getCheckinComment();
                        }
                    } catch (DocumentNotFoundException e) {
                        // If document not found, versionLabel and
                        // checkimComment fields will remain empty.
                        log.trace(logInitMsg
                                + "Document not found for versionDocId = |"
                                + versionDocId + "|");
                    }
                }

                StateLogDisplay stateLogDisplay = new StateLogDisplay(
                        stateLog.getUser(), stateLog.getDate(),
                        stateLog.getStateFrom(), stateLog.getStateTo(),
                        stateLog.getTransition(), versionDocId,
                        stateLog.getComment(), versionLabel, checkinComment);
                stateLogDisplayList.add(stateLogDisplay);
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        return stateLogDisplayList;
    }

}
