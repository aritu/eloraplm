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
package com.aritu.eloraplm.doctypes.datatable;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.ProjectConstants;
import com.aritu.eloraplm.datatable.EditableTableBean;
import com.aritu.eloraplm.datatable.RowData;

/**
 *
 * @author aritu
 *
 */
@Name("projectPhasesTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ProjectPhasesTableBean extends EditableTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ProjectPhasesTableBean.class);

    private static final String[] PHASE_TYPES = {
            ProjectConstants.PROJECT_PHASE_TYPE_PHASE,
            ProjectConstants.PROJECT_PHASE_TYPE_SUBPHASE,
            ProjectConstants.PROJECT_PHASE_TYPE_GATE };

    private List<String> phaseTypes;

    /* Add/Edit fields */

    private String type = ProjectConstants.PROJECT_PHASE_TYPE_PHASE;

    private String parentId;

    private String description;

    private String manager;

    private boolean isDeliverableRequired;

    private String deliverableName;

    private Date realStartDate;

    private Date plannedEndDate;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public boolean getIsDeliverableRequired() {
        return isDeliverableRequired;
    }

    public void setIsDeliverableRequired(boolean isDeliverableRequired) {
        this.isDeliverableRequired = isDeliverableRequired;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public Date getRealStartDate() {
        return realStartDate;
    }

    public void setRealStartDate(Date realStartDate) {
        this.realStartDate = realStartDate;
    }

    public Date getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(Date plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }
    /* ---------------- */

    // To be able to negate stream filter
    private static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }

    public ProjectPhasesTableBean() {
        tableService = new ProjectPhasesTableServiceImpl();
        phaseTypes = Arrays.asList(PHASE_TYPES);
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(getCurrentDocument()));
            setIsDirty(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    @Factory(value = "projectPhasesData", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Override
    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding row to phases datatable of project |"
                + getCurrentDocument().getId() + "|.");

        try {
            ProjectPhasesTableServiceImpl ts = (ProjectPhasesTableServiceImpl) tableService;

            // Empty parentId if type is not subphase
            if (!getType().equals(
                    ProjectConstants.PROJECT_PHASE_TYPE_SUBPHASE)) {
                setParentId(null);
            }

            String rowId = calculateRowId();

            Map<String, Object> deliverable = new HashMap<String, Object>();
            deliverable.put(
                    ProjectConstants.PROJECT_PHASE_DELIVERABLES_ISREQUIRED,
                    getIsDeliverableRequired());
            deliverable.put(ProjectConstants.PROJECT_PHASE_DELIVERABLES_NAME,
                    getDeliverableName());

            RowData row = ts.createRowData(rowId, getParentId(), getType(),
                    getDescription(), getManager(), deliverable,
                    getRealStartDate(), getPlannedEndDate(), null, 0, null,
                    true, false, false);

            getData().add(row);

            log.trace(logInitMsg + "Row |" + rowId + "| added.");

            resetCreateFormValues();
            setIsDirty(true);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.datatable.row.add"));
        } catch (

        Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.datatable.row.add"));
        }
    }

    private String calculateRowId() {
        String rowId;
        List<String> rowIdList = getRowIdList();

        if (getParentId() == null) {
            String maxRowId = rowIdList.stream().filter(
                    not(x -> x.contains("_"))).max(String::compareTo).orElse(
                            null);
            if (maxRowId == null) {
                rowId = "001";
            } else {
                int m = Integer.valueOf(maxRowId);
                rowId = String.format("%03d", (m + 1));
            }
        } else {
            String maxRowId = rowIdList.stream().filter(
                    x -> x.startsWith(getParentId() + "_")).max(
                            String::compareTo).orElse(null);
            if (maxRowId == null) {
                rowId = getParentId() + "_001";
            } else {
                String[] a = maxRowId.split("_");
                int m = Integer.valueOf(a[a.length - 1]);
                rowId = getParentId() + "_" + String.format("%03d", (m + 1));
            }
        }
        return rowId;
    }

    private List<String> getRowIdList() {
        List<String> rowIdList = new ArrayList<String>();
        for (RowData r : getData()) {
            ProjectPhaseRowData row = (ProjectPhaseRowData) r;
            if (row.getId() != null) {
                rowIdList.add(row.getId());
            }
        }
        return rowIdList;
    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (getIsDirty()) {
            try {

                DocumentModel doc = getCurrentDocument();
                if (doc.isProxy()) {
                    doc = documentManager.getWorkingCopy(doc.getRef());
                }

                List<HashMap<String, Object>> phaseList = new ArrayList<HashMap<String, Object>>();
                for (RowData r : getData()) {
                    ProjectPhaseRowData row = (ProjectPhaseRowData) r;
                    if (!row.getIsRemoved()) {
                        HashMap<String, Object> phase = new HashMap<String, Object>();
                        phase.put(ProjectConstants.PROJECT_PHASE_ID,
                                row.getId());
                        phase.put(ProjectConstants.PROJECT_PHASE_PARENTID,
                                row.getParentId());
                        phase.put(ProjectConstants.PROJECT_PHASE_TYPE,
                                row.getType());
                        phase.put(ProjectConstants.PROJECT_PHASE_DESCRIPTION,
                                row.getDescription());
                        phase.put(ProjectConstants.PROJECT_PHASE_MANAGER,
                                row.getManager());

                        List<Map<String, Object>> dlvs = new ArrayList<Map<String, Object>>();
                        Map<String, Object> dlv = row.getDeliverable();
                        if (dlv != null) {
                            /**
                             * BETTER TO LINK THE PROXY, BECAUSE THAT IS WHAT
                             * THEY WORK WITH // Normally the doc will be a
                             * proxy (is what the // page provider returns). Get
                             * the source doc. if (dlv.get(
                             * ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENT)
                             * != null) { String dlvDocId = (String) dlv.get(
                             * ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENT);
                             * DocumentModel dlvDoc =
                             * documentManager.getDocument( new
                             * IdRef(dlvDocId)); if (dlvDoc.isProxy()) {
                             * dlv.put(ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENT,
                             * dlvDoc.getSourceId()); } }
                             */
                            dlvs.add(dlv);
                        }
                        phase.put(ProjectConstants.PROJECT_PHASE_DELIVERABLES,
                                dlvs);

                        phase.put(ProjectConstants.PROJECT_PHASE_REALSTARTDATE,
                                row.getRealStartDate());
                        phase.put(ProjectConstants.PROJECT_PHASE_PLANNEDENDDATE,
                                row.getPlannedEndDate());
                        phase.put(ProjectConstants.PROJECT_PHASE_REALENDDATE,
                                row.getRealEndDate());
                        phase.put(ProjectConstants.PROJECT_PHASE_PROGRESS,
                                row.getProgress());
                        phase.put(ProjectConstants.PROJECT_PHASE_COMMENT,
                                row.getComment());
                        phaseList.add(phase);

                        row.setIsModified(false);
                    }
                }
                doc.setPropertyValue(
                        EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST,
                        (Serializable) phaseList);
                doc = documentManager.saveDocument(doc);
                documentManager.save();
                doc.refresh();
                createData();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.project.phases.save"));
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.project.phases.save"));
            }
        }
    }

    public List<String> getPhaseTypes() {
        return phaseTypes;
    }

    public Map<String, String> getParentPhases() {
        Map<String, String> parentPhases = new TreeMap<String, String>();
        for (RowData r : getData()) {
            ProjectPhaseRowData row = (ProjectPhaseRowData) r;
            String type = row.getType();
            if (type != null) {
                if (!type.equals(ProjectConstants.PROJECT_PHASE_TYPE_GATE)) {
                    if (row.getId() != null && row.getDescription() != null) {
                        parentPhases.put(row.getId(), row.getDescription());
                    }
                }
            }
        }
        return parentPhases;
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        type = "phase";
        parentId = null;
        description = null;
        manager = null;
        isDeliverableRequired = false;
        deliverableName = null;
        realStartDate = null;
        plannedEndDate = null;
    }
}
