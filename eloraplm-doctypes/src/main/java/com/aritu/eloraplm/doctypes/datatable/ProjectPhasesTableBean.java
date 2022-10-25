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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.ProjectConstants;
import com.aritu.eloraplm.constants.QueriesConstants;
import com.aritu.eloraplm.datatable.EditableDocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 *
 * @author aritu
 *
 */
@Name("projectPhasesTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ProjectPhasesTableBean extends EditableDocBasedTableBean
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

    private String title;

    private String description;

    private String manager;

    private List<Map<String, Object>> deliverables;

    private Date realStartDate;

    private Date plannedEndDate;

    private String fromManager;

    private String toManager;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<Map<String, Object>> getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(List<Map<String, Object>> deliverables) {
        this.deliverables = deliverables;
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

    public String getFromManager() {
        return fromManager;
    }

    public void setFromManager(String fromManager) {
        this.fromManager = fromManager;
    }

    public String getToManager() {
        return toManager;
    }

    public void setToManager(String toManager) {
        this.toManager = toManager;
    }

    /* ---------------- */

    // To be able to negate stream filter
    private static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }

    public ProjectPhasesTableBean() {
        tableService = new ProjectPhasesTableServiceImpl();
        phaseTypes = Arrays.asList(PHASE_TYPES);
        deliverables = new ArrayList<Map<String, Object>>();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(getCurrentDocument()));
            setIsDirty(false);
            log.trace(logInitMsg + "Table created.");
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

            RowData row = ts.createRowData(rowId, getParentId(), getType(),
                    getTitle(), getDescription(), getManager(), deliverables,
                    getRealStartDate(), getPlannedEndDate(), null, 0, null,
                    null, false, true, false, false);

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
            String maxRowId = rowIdList.stream().filter(not(x -> x.contains(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR))).max(
                            String::compareTo).orElse(null);
            if (maxRowId == null) {
                rowId = ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID;
            } else {
                int m = Integer.valueOf(maxRowId);
                rowId = String.format("%03d", (m + 1));
            }
        } else {
            String maxRowId = rowIdList.stream().filter(
                    x -> x.startsWith(getParentId()
                            + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR)).max(
                                    String::compareTo).orElse(null);
            if (maxRowId == null) {
                rowId = getParentId()
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID;
            } else {
                String[] a = maxRowId.split(
                        ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
                int m = Integer.valueOf(a[a.length - 1]);
                rowId = getParentId()
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + String.format("%03d", (m + 1));
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
                        phase.put(ProjectConstants.PROJECT_PHASE_TITLE,
                                row.getTitle());
                        phase.put(ProjectConstants.PROJECT_PHASE_DESCRIPTION,
                                row.getDescription());
                        phase.put(ProjectConstants.PROJECT_PHASE_MANAGER,
                                row.getManager());
                        phase.put(ProjectConstants.PROJECT_PHASE_DELIVERABLES,
                                row.getDeliverables());
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
                        phase.put(ProjectConstants.PROJECT_PHASE_RESULT,
                                row.getResult());
                        phase.put(ProjectConstants.PROJECT_PHASE_OBSOLETE,
                                row.getObsolete());
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

    public void modifyManager() {
        String logInitMsg = "[modifyManager] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg
                + "Modifying the manager of all the phases of the project |"
                + getCurrentDocument().getId() + "|.");
        try {
            String fromManager = getFromManager();
            String toManager = getToManager();
            if (fromManager != null && fromManager.length() > 0
                    && toManager != null && toManager.length() > 0) {
                if (!fromManager.equals(toManager)) {
                    for (RowData r : getData()) {
                        ProjectPhaseRowData row = (ProjectPhaseRowData) r;
                        if (!row.getIsRemoved()) {
                            String currentManager = row.getManager();
                            if (currentManager != null
                                    && currentManager.equals(fromManager)) {
                                row.setManager(toManager);
                                row.setIsModified(true);
                            }
                        }
                    }
                    setIsDirty(true);
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.project.phases.manager.modified"));
                } else {
                    log.trace(logInitMsg
                            + "Specified fromManager and toManager have same avalue: fromManager = |"
                            + fromManager + "|, toManager = |" + toManager
                            + "|");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.warning.project.phase.modify.manager.sameValues"));
                }
            } else {
                log.trace(logInitMsg
                        + "Specified fromManager or toManager cannot be empty: fromManager = |"
                        + fromManager + "|, toManager = |" + toManager + "|");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.project.phase.modify.manager.emptyValue"));
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.phases.manager.modify"));
        } finally {
            resetModifyManagerFormValues();
        }
    }

    public Map<String, String> loadContentDocuments() {
        String logInitMsg = "[loadContentDocuments] ["
                + documentManager.getPrincipal().getName() + "] ";

        Map<String, String> documentList = new LinkedHashMap<String, String>();

        try {
            DocumentModel currentDocument = getCurrentDocument();

            String contentDocumentListsQuery = EloraQueryFactory.getOtherDocumentProxiesInsideAncestorQuery(
                    currentDocument.getId());

            DocumentModelList contentDocumentList = documentManager.query(
                    contentDocumentListsQuery);

            if (contentDocumentList != null && contentDocumentList.size() > 0) {
                for (DocumentModel contentDoc : contentDocumentList) {

                    String lifeCycleState = messages.get(
                            contentDoc.getCurrentLifeCycleState()
                                    + EloraLifeCycleConstants.ABBR_SUFFIX);
                    documentList.put(contentDoc.getId(), contentDoc.getTitle()
                            + "  [" + lifeCycleState + "]");
                }
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.load.content.documents"));
        }

        return documentList;
    }

    public Map<String, String> getProjectDeliverableDocumentVersions(
            String documentId) throws EloraException {
        String logInitMsg = "[getProjectDeliverableDocumentVersions] ["
                + documentManager.getPrincipal().getName() + "] ";

        Map<String, String> versionList = new LinkedHashMap<String, String>();

        try {
            if (documentId != null) {
                DocumentModel document = documentManager.getDocument(
                        new IdRef(documentId));

                if (document != null && !document.isImmutable()) {
                    if (document.isProxy()) {
                        document = documentManager.getWorkingCopy(
                                document.getRef());
                    }
                    // calculate the version list
                    String allVersionsDocsQuery = EloraQueryFactory.getAllVersionsDocsQuery(
                            document.getType(), document.getId(), false,
                            QueriesConstants.SORT_ORDER_DESC);

                    DocumentModelList allVersionDocs = documentManager.query(
                            allVersionsDocsQuery);

                    if (allVersionDocs != null && allVersionDocs.size() > 0) {
                        for (DocumentModel versionDoc : allVersionDocs) {
                            versionList.put(versionDoc.getId(),
                                    versionDoc.getVersionLabel());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.get.deliverable.document.versions"));
        }

        return versionList;
    }

    public void chooseDeliverableDocument() {
        String logInitMsg = "[chooseDeliverableDocument] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            String dataTableRowIndexParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "dataTableRowIndex");
            String deliverableRowIndexParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "deliverableRowIndex");
            String documentIdParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "documentId");

            log.trace(logInitMsg + "dataTableRowIndexParam = |"
                    + dataTableRowIndexParam + "|, deliverableRowIndexParam = |"
                    + deliverableRowIndexParam + "|, documentIdParam = |"
                    + documentIdParam + "|");

            if (dataTableRowIndexParam != null
                    && deliverableRowIndexParam != null
                    && documentIdParam != null) {

                int dataTableRowIndex = Integer.parseInt(
                        dataTableRowIndexParam);
                int deliverableRowIndex = Integer.parseInt(
                        deliverableRowIndexParam);

                ProjectPhaseRowData row = (ProjectPhaseRowData) getData().get(
                        dataTableRowIndex);
                Map<String, Object> deliverable = row.getDeliverables().get(
                        deliverableRowIndex);
                if (documentIdParam != null && documentIdParam.length() > 0) {
                    DocumentModel selectedDocM = documentManager.getDocument(
                            new IdRef(documentIdParam));
                    if (selectedDocM != null) {
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY,
                                documentIdParam);
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTAV,
                                null);
                    } else {
                        log.error(logInitMsg
                                + "Selected document cannot be retrieved: documentId = |"
                                + documentIdParam + "|.");
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_ANCHORINGMSG,
                                messages.get(
                                        "eloraplm.message.error.project.choose.deliverable.document.documentNotFound"));
                    }
                } else {
                    // empty document
                    deliverable.put(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY,
                            null);
                    deliverable.put(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTAV,
                            null);
                }
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.choose.deliverable.document"));
        }
    }

    public void chooseDeliverableDocumentVersion() {
        String logInitMsg = "[chooseDeliverableDocumentVersion] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            String dataTableRowIndexParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "dataTableRowIndex");
            String deliverableRowIndexParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "deliverableRowIndex");
            String documentIdParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
                    "documentId");

            log.trace(logInitMsg + "dataTableRowIndexParam = |"
                    + dataTableRowIndexParam + "|, deliverableRowIndexParam = |"
                    + deliverableRowIndexParam + "|, documentIdParam = |"
                    + documentIdParam + "|");

            if (dataTableRowIndexParam != null
                    && deliverableRowIndexParam != null
                    && documentIdParam != null) {

                int dataTableRowIndex = Integer.parseInt(
                        dataTableRowIndexParam);
                int deliverableRowIndex = Integer.parseInt(
                        deliverableRowIndexParam);

                ProjectPhaseRowData row = (ProjectPhaseRowData) getData().get(
                        dataTableRowIndex);
                Map<String, Object> deliverable = row.getDeliverables().get(
                        deliverableRowIndex);

                if (documentIdParam != null && documentIdParam.length() > 0) {
                    DocumentModel selectedDocM = documentManager.getDocument(
                            new IdRef(documentIdParam));

                    if (selectedDocM != null) {
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTAV,
                                documentIdParam);
                    } else {
                        log.error(logInitMsg
                                + "Selected document cannot be retrieved: documentId = |"
                                + documentIdParam + "|.");
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_ANCHORINGMSG,
                                messages.get(
                                        "eloraplm.message.error.project.choose.deliverable.document.documentNotFound"));
                    }
                } else {
                    // empty document version
                    deliverable.put(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTAV,
                            "");

                    // Check that the anchoredDocWcProxyUid still exists in the
                    // project content
                    String anchoredDocWcProxyUid = (String) deliverable.get(
                            ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY);
                    String currentDocumentId = getCurrentDocument().getId();
                    String documentInProjectContentQuery = EloraQueryFactory.getDocumentInsideAncestorQuery(
                            anchoredDocWcProxyUid, currentDocumentId);

                    DocumentModelList documentInProjectContent = documentManager.query(
                            documentInProjectContentQuery);

                    if (documentInProjectContent == null
                            || documentInProjectContent.size() == 0) {
                        // empty also the proxy, since it doesn't exist any more
                        // on the content
                        deliverable.put(
                                ProjectConstants.PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY,
                                "");

                        log.trace(logInitMsg + "The proxy |"
                                + anchoredDocWcProxyUid
                                + "| does not exist in the content of the document |"
                                + currentDocumentId
                                + "|, so it will be emptied.");
                    }
                }
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.project.choose.deliverable.document.version"));
        }
    }

    public boolean isFirstRow(String currentRowId) {
        boolean isFirstRow = false;
        if (currentRowId.indexOf(
                ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
            if (currentRowId.equals(
                    ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID)) {
                isFirstRow = true;
            }
        } else {
            int separatorLastIndex = currentRowId.lastIndexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
            String currentLevelRowId = currentRowId.substring(
                    separatorLastIndex + 1, currentRowId.length());
            if (currentLevelRowId.equals(
                    ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID)) {
                isFirstRow = true;
            }
        }
        return isFirstRow;
    }

    public boolean isLastRow(String currentRowId) {
        boolean isLastRow = false;

        if (currentRowId.indexOf(
                ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
            String lastRowId = getLastRowId(null);
            if (currentRowId.equals(lastRowId)) {
                isLastRow = true;
            }
        } else {
            int separatorLastIndex = currentRowId.lastIndexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
            String parentLevelRowId = currentRowId.substring(0,
                    separatorLastIndex);

            String lastRowId = getLastRowId(parentLevelRowId);
            if (currentRowId.equals(lastRowId)) {
                isLastRow = true;
            }
        }
        return isLastRow;
    }

    public void moveRow(String direction, String currentRowId) {
        String logInitMsg = "[moveRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "direction = |" + direction
                + "|, currentRowId = |" + currentRowId + "|");
        // move TOP
        if (direction != null
                && direction.equals(ProjectConstants.PROJECT_PHASE_MOVE_TOP)) {
            String previousRowId = null;

            // first level elements
            if (currentRowId.indexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
                // check that it is not already at the first position
                if (!currentRowId.equals(
                        ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID)) {
                    int currentRowIdInt = Integer.valueOf(currentRowId);
                    int previousRowIdInt = currentRowIdInt - 1;
                    previousRowId = String.format("%03d", previousRowIdInt);
                }
            }
            // other level elements
            else {
                String[] currentRowIdSplitted = currentRowId.split(
                        ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
                String currentLevelRowId = currentRowIdSplitted[currentRowIdSplitted.length
                        - 1];
                // check that it is not already at the first position
                if (!currentLevelRowId.equals(
                        ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID)) {
                    int currentLevelRowIdInt = Integer.valueOf(
                            currentLevelRowId);
                    int currentLevelPreviousRowIdInt = currentLevelRowIdInt - 1;
                    String parentLevelRowId = currentRowIdSplitted[currentRowIdSplitted.length
                            - 2];
                    previousRowId = parentLevelRowId
                            + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                            + String.format("%03d",
                                    currentLevelPreviousRowIdInt);
                }
            }
            if (previousRowId != null) {
                updateTableRowsIds(previousRowId, currentRowId, currentRowId);
                setIsDirty(true);
            }
        }
        // move DOWN
        else if (direction != null
                && direction.equals(ProjectConstants.PROJECT_PHASE_MOVE_DOWN)) {
            String nextRowId = null;
            // first level elements
            if (currentRowId.indexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
                // check that it is not already at the last position
                String lastRowId = getLastRowId(null);
                if (!currentRowId.equals(lastRowId)) {
                    int currentRowIdInt = Integer.valueOf(currentRowId);
                    int nextRowIdInt = currentRowIdInt + 1;
                    nextRowId = String.format("%03d", nextRowIdInt);
                }
            }
            // other level elements
            else {
                int separatorLastIndex = currentRowId.lastIndexOf(
                        ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
                String currentLevelRowId = currentRowId.substring(
                        separatorLastIndex + 1, currentRowId.length());
                String parentLevelRowId = currentRowId.substring(0,
                        separatorLastIndex);

                String lastRowId = getLastRowId(parentLevelRowId);

                if (!currentLevelRowId.equals(lastRowId)) {
                    int currentLevelRowIdInt = Integer.valueOf(
                            currentLevelRowId);
                    int currentLevelNextRowIdInt = currentLevelRowIdInt + 1;
                    nextRowId = parentLevelRowId
                            + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                            + String.format("%03d", currentLevelNextRowIdInt);
                }
            }
            if (nextRowId != null) {
                updateTableRowsIds(currentRowId, nextRowId, currentRowId);
                setIsDirty(true);
            }
        }
    }

    /**
     * If parentRow is null, retrieve the last row id of the datatable.
     * Otherwise, the last row id under the specified parent row id.
     *
     * @param parentRow
     * @return
     */
    private String getLastRowId(String parentRowId) {
        String rowId;
        List<String> rowIdList = getRowIdList();

        if (parentRowId == null) {
            String maxRowId = rowIdList.stream().filter(not(x -> x.contains(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR))).max(
                            String::compareTo).orElse(null);
            if (maxRowId == null) {
                rowId = ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID;
            } else {
                rowId = String.format("%03d", Integer.valueOf(maxRowId));
            }
        } else {
            String maxRowId = rowIdList.stream().filter(
                    x -> x.startsWith(parentRowId
                            + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR)).max(
                                    String::compareTo).orElse(null);
            if (maxRowId == null) {
                rowId = parentRowId
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + ProjectConstants.PROJECT_PHASE_FIRST_ROW_ID;
            } else {
                String[] a = maxRowId.split(
                        ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
                rowId = parentRowId
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + String.format("%03d",
                                Integer.valueOf(a[a.length - 1]));
            }
        }
        return rowId;
    }

    private void updateTableRowsIds(String rowIdToBeIncreased,
            String rowIdToBeDecreased, String rowIdBeingMoved) {
        String logInitMsg = "[updateTableRowsIdsMoveTop] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "rowIdToBeIncreased = |" + rowIdToBeIncreased
                + "|, rowIdToBeDecreased = |" + rowIdToBeDecreased + "|");

        for (RowData r : getData()) {
            ProjectPhaseRowData row = (ProjectPhaseRowData) r;
            if (!row.getIsRemoved()) {

                String rowId = row.getId();
                if (rowId.equals(rowIdToBeIncreased)) {
                    row.setId(increaseRowId(rowId));
                } else if (rowId.startsWith(rowIdToBeIncreased)) {
                    row.setId(increaseRowId(rowId, true));
                } else if (rowId.equals(rowIdToBeDecreased)) {
                    row.setId(decreaseRowId(rowId));
                } else if (rowId.startsWith(rowIdToBeDecreased)) {
                    row.setId(decreaseRowId(rowId, true));
                }

                // Mark as modified only the row that is being moved
                if (rowId.equals(rowIdBeingMoved)) {
                    row.setIsModified(true);
                }
            }
        }
    }

    private String increaseRowId(String rowId) {
        return increaseRowId(rowId, false);
    }

    private String increaseRowId(String rowId, boolean increaseParent) {
        String logInitMsg = "[increaseRowId] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "rowId = |" + rowId + "|");

        String newRowId;

        if (rowId.indexOf(
                ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
            newRowId = String.format("%03d", Integer.valueOf(rowId) + 1);
        } else {
            int separatorLastIndex = rowId.lastIndexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
            String currentLevelRowId = rowId.substring(separatorLastIndex + 1,
                    rowId.length());
            String parentLevelRowId = rowId.substring(0, separatorLastIndex);

            if (increaseParent) {
                newRowId = String.format("%03d",
                        Integer.valueOf(parentLevelRowId) + 1)
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + currentLevelRowId;
            } else {
                newRowId = parentLevelRowId
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + String.format("%03d",
                                Integer.valueOf(currentLevelRowId) + 1);
            }
        }
        log.trace(logInitMsg + "newRowId = |" + newRowId + "|");
        return newRowId;
    }

    private String decreaseRowId(String rowId) {
        return decreaseRowId(rowId, false);
    }

    private String decreaseRowId(String rowId, boolean decreaseParent) {
        String logInitMsg = "[decreaseRowId] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "rowId = |" + rowId + "|");

        String newRowId;

        if (rowId.indexOf(
                ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR) == -1) {
            newRowId = String.format("%03d", Integer.valueOf(rowId) - 1);
        } else {
            int separatorLastIndex = rowId.lastIndexOf(
                    ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR);
            String currentLevelRowId = rowId.substring(separatorLastIndex + 1,
                    rowId.length());
            String parentLevelRowId = rowId.substring(0, separatorLastIndex);
            if (decreaseParent) {
                newRowId = String.format("%03d",
                        Integer.valueOf(parentLevelRowId) - 1)
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + currentLevelRowId;
            } else {
                newRowId = parentLevelRowId
                        + ProjectConstants.PROJECT_PHASE_ROW_ID_SEPARATOR
                        + String.format("%03d",
                                Integer.valueOf(currentLevelRowId) - 1);
            }
        }

        log.trace(logInitMsg + "newRowId = |" + newRowId + "|");
        return newRowId;
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
                if (type.equals(ProjectConstants.PROJECT_PHASE_TYPE_PHASE)) {
                    if (row.getId() != null && row.getTitle() != null
                            && !row.getObsolete()) {
                        parentPhases.put(row.getId(), row.getTitle());
                    }
                }
            }
        }
        return parentPhases;
    }

    public void propagateDate(ProjectPhaseRowData row, String field) {
        String id = row.getId();
        switch (field) {
        case "realStartDate":
            Date realStartDate = row.getRealStartDate();
            getData().stream().filter(r -> !r.getId().equals(id)
                    && r.getId().startsWith(id)).forEach(a -> {
                        ((ProjectPhaseRowData) a).setRealStartDate(
                                realStartDate);
                        a.setIsModified(true);
                    });
            break;
        case "realEndDate":
            Date realEndDate = row.getRealEndDate();
            getData().stream().filter(r -> !r.getId().equals(id)
                    && r.getId().startsWith(id)).forEach(a -> {
                        ((ProjectPhaseRowData) a).setRealEndDate(realEndDate);
                        a.setIsModified(true);
                    });
            break;
        case "plannedEndDate":
            Date plannedEndDate = row.getPlannedEndDate();
            getData().stream().filter(r -> !r.getId().equals(id)
                    && r.getId().startsWith(id)).forEach(a -> {
                        ((ProjectPhaseRowData) a).setPlannedEndDate(
                                plannedEndDate);
                        a.setIsModified(true);
                    });
            break;
        }

        setIsDirty(true);
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        type = "phase";
        parentId = null;
        title = null;
        description = null;
        manager = null;
        deliverables = new ArrayList<Map<String, Object>>();
        realStartDate = null;
        plannedEndDate = null;
    }

    protected void resetModifyManagerFormValues() {
        fromManager = null;
        toManager = null;
    }
}
