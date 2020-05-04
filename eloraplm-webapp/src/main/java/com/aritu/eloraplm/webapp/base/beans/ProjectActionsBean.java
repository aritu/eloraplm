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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.ProjectConstants;

/**
 * @author aritu
 *
 */

@Name("projectActions")
@Scope(EVENT)
public class ProjectActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public void updateProgress() {
        DocumentModel doc = navigationContext.getCurrentDocument();

        try {
            doc = documentManager.saveDocument(doc);
        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.progress.update"));
        }
        documentManager.save();
        // some changes (versioning) happened server-side, fetch new one
        navigationContext.invalidateCurrentDocument();
        facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                "eloraplm.message.success.project.progress.update"));
    }

    public String getCurrentPhase(DocumentModel doc) {
        String currentPhase = "";
        if (doc != null && doc.getPropertyValue(
                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST) != null) {
            @SuppressWarnings("unchecked")
            ArrayList<HashMap<String, Object>> phaseList = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST);
            for (HashMap<String, Object> phase : phaseList) {
                String phaseType = phase.get(
                        ProjectConstants.PROJECT_PHASE_TYPE) != null
                                ? (String) phase.get(
                                        ProjectConstants.PROJECT_PHASE_TYPE)
                                : ProjectConstants.PROJECT_PHASE_TYPE_PHASE;
                if (phaseType.equals(ProjectConstants.PROJECT_PHASE_TYPE_PHASE)
                        || phaseType.equals(
                                ProjectConstants.PROJECT_PHASE_TYPE_GATE)) {
                    long progress = (Long) phase.get(
                            ProjectConstants.PROJECT_PHASE_PROGRESS);
                    if (progress < 100L) {
                        currentPhase = (String) phase.get(
                                ProjectConstants.PROJECT_PHASE_DESCRIPTION);
                        break;
                    }
                }
            }

        }

        return currentPhase;
    }

    public Date getPhaseRealStartDate(DocumentModel doc, String phaseId) {
        GregorianCalendar cal = null;
        if (doc != null && doc.getPropertyValue(
                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST) != null) {
            HashMap<String, Object> phase = getPhaseById(doc, phaseId);
            if (phase != null) {
                cal = (GregorianCalendar) phase.get("phaseRealStartDate");
            }
        }
        return cal != null ? cal.getTime() : null;
    }

    public Date getPhasePlannedEndDate(DocumentModel doc, String phaseId) {
        GregorianCalendar cal = null;
        if (doc != null && doc.getPropertyValue(
                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST) != null) {
            HashMap<String, Object> phase = getPhaseById(doc, phaseId);
            if (phase != null) {
                cal = (GregorianCalendar) phase.get("phasePlannedEndDate");
            }
        }
        return cal != null ? cal.getTime() : null;
    }

    private HashMap<String, Object> getPhaseById(DocumentModel doc,
            String phaseId) {
        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> phaseList = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST);

        try {
            HashMap<String, Object> phase = phaseList.stream().filter(
                    x -> x.get(ProjectConstants.PROJECT_PHASE_ID).equals(
                            phaseId)).findFirst().orElse(null);

            return phase;

        } catch (Exception e) {
            return null;
        }
    }

}
