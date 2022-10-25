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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */

@Name("projectActions")
@Scope(EVENT)
public class ProjectActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ProjectActionsBean.class);

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

    /**
     * Returns the current phase of the project, which is the first phases that
     * is not 100% completed.
     *
     * @param doc
     * @return first phase that is not 100% completed
     */
    private Map<String, Object> getCurrentPhase(DocumentModel doc) {

        Map<String, Object> currentPhase = null;

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
                if (phaseType.equals(
                        ProjectConstants.PROJECT_PHASE_TYPE_PHASE)) {
                    Long progress = (Long) phase.get(
                            ProjectConstants.PROJECT_PHASE_PROGRESS);
                    if (progress != null && progress < 100L) {
                        currentPhase = phase;
                        break;
                    }
                }
            }
            if (currentPhase == null && !phaseList.isEmpty()) {
                currentPhase = phaseList.get(0);
            }
        }
        return currentPhase;
    }

    public String getCurrentPhaseTitle(DocumentModel doc) {
        String currentPhaseTitle = "";

        Map<String, Object> currentPhase = getCurrentPhase(doc);

        if (currentPhase != null) {
            currentPhaseTitle = (String) currentPhase.get(
                    ProjectConstants.PROJECT_PHASE_TITLE);
        }

        return currentPhaseTitle;
    }

    public String getCurrentPhaseDescription(DocumentModel doc) {
        String currentPhaseDescription = "";

        Map<String, Object> currentPhase = getCurrentPhase(doc);

        if (currentPhase != null) {
            currentPhaseDescription = (String) currentPhase.get(
                    ProjectConstants.PROJECT_PHASE_DESCRIPTION);
        }

        return currentPhaseDescription;
    }

    public String getCurrentPhaseManager(DocumentModel doc) {
        String currentPhaseManager = "";

        Map<String, Object> currentPhase = getCurrentPhase(doc);

        if (currentPhase != null) {
            currentPhaseManager = (String) currentPhase.get(
                    ProjectConstants.PROJECT_PHASE_MANAGER);
        }

        return currentPhaseManager;
    }

    public Date getCurrentPhaseEndDate(DocumentModel doc) {
        GregorianCalendar cal = null;

        Map<String, Object> currentPhase = getCurrentPhase(doc);

        if (currentPhase != null) {
            cal = (GregorianCalendar) currentPhase.get(
                    ProjectConstants.PROJECT_PHASE_PLANNEDENDDATE);
        }

        return cal != null ? cal.getTime() : null;
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

    /**
     * Returns project phases, excluding sub-phases and gates. Obsolete phases
     * are not returned.
     *
     * @return
     * @throws EloraException
     */
    public ArrayList<HashMap<String, Object>> getPhasesList()
            throws EloraException {

        return getPhasesList(false);
    }

    /**
     * Returns project phases, excluding sub-phases and gates.
     *
     * @param includeObsoletePhases indicates if obsolete phases should be
     *            include or not in the result.
     * @return
     * @throws EloraException
     */
    public ArrayList<HashMap<String, Object>> getPhasesList(
            boolean includeObsoletePhases) throws EloraException {

        String logInitMsg = "[getPhasesList] ";
        ArrayList<HashMap<String, Object>> phasesList = new ArrayList<HashMap<String, Object>>();

        try {
            ArrayList<HashMap<String, Object>> projectPhasesList = new ArrayList<HashMap<String, Object>>();

            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            if (currentDocument.getPropertyValue(
                    EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST) != null) {
                projectPhasesList.addAll(
                        (ArrayList<HashMap<String, Object>>) currentDocument.getPropertyValue(
                                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST));
            }

            for (HashMap<String, Object> projectPhase : projectPhasesList) {
                String type = (String) projectPhase.get("type");
                if (type != null && type.equals(
                        ProjectConstants.PROJECT_PHASE_TYPE_PHASE)) {
                    if (!includeObsoletePhases) {
                        boolean obsolete = (boolean) projectPhase.get(
                                "obsolete");
                        if (!obsolete) {
                            phasesList.add(projectPhase);
                        }
                    } else {
                        phasesList.add(projectPhase);
                    }
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        return phasesList;
    }

    /**
     * Returns the deliverables status of the specified set of deliverables.
     *
     * @param deliverables
     * @return
     */
    public String getDeliverablesStatus(
            List<Map<String, Object>> deliverables) {

        String logInitMsg = "[getDeliverablesStatus] ";

        boolean not_defined = false;
        boolean not_anchored = false;
        boolean all_optional = true;

        try {
            for (Iterator<Map<String, Object>> deliverablesIt = deliverables.iterator(); deliverablesIt.hasNext();) {
                Map<String, Object> deliverable = deliverablesIt.next();
                boolean isRequired = (boolean) deliverable.get(
                        ProjectConstants.PROJECT_PHASE_DELIVERABLES_ISREQUIRED);
                if (isRequired) {
                    all_optional = false;
                    String docWcProxy = (String) deliverable.get(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY);
                    String link = (String) deliverable.get(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_LINK);
                    if ((docWcProxy == null || docWcProxy.length() == 0)
                            && (link == null || link.length() == 0)) {
                        not_defined = true;
                    } else {
                        if (link == null || link.length() == 0) {

                            if (docWcProxy != null && docWcProxy.length() > 0) {
                                String docAv = (String) deliverable.get(
                                        ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTAV);
                                if (docAv == null || docAv.length() == 0) {
                                    not_anchored = true;

                                }
                            }
                        }
                    }
                }
            }

            if (all_optional) {
                return ProjectConstants.PROJECT_PHASE_DELIVERABLES_STATUS_ALL_OPTIONAL;
            } else if (not_defined) {
                return ProjectConstants.PROJECT_PHASE_DELIVERABLES_STATUS_NOT_DEFINED;
            } else if (not_anchored) {
                return ProjectConstants.PROJECT_PHASE_DELIVERABLES_STATUS_NOT_ANCHORED;
            } else {
                return ProjectConstants.PROJECT_PHASE_DELIVERABLES_STATUS_COMPLETED;
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            return "";
        }
    }

}
