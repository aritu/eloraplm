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

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.ProjectConstants;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.datatable.TableService;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class ProjectPhasesTableServiceImpl implements TableService {

    @Override
    public List<RowData> getData(Object parentObject) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;

        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> phaseList = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_PRJ_PROJECTPHASELIST);

        for (HashMap<String, Object> phase : phaseList) {
            String rowId = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_ID);
            String parentId = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_PARENTID);
            String type = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_TYPE);
            String description = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_DESCRIPTION);
            String manager = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_MANAGER);
            Map<String, Object> deliverable = getDeliverable(phase);
            Date realStartDate = getPhaseDateProperty(phase,
                    ProjectConstants.PROJECT_PHASE_REALSTARTDATE);
            Date plannedEndDate = getPhaseDateProperty(phase,
                    ProjectConstants.PROJECT_PHASE_PLANNEDENDDATE);
            Date realEndDate = getPhaseDateProperty(phase,
                    ProjectConstants.PROJECT_PHASE_REALENDDATE);
            int progress = getPhaseIntegerProperty(phase,
                    ProjectConstants.PROJECT_PHASE_PROGRESS);
            String comment = getPhaseStringProperty(phase,
                    ProjectConstants.PROJECT_PHASE_COMMENT);

            data.add(createRowData(rowId, parentId, type, description, manager,
                    deliverable, realStartDate, plannedEndDate, realEndDate,
                    progress, comment, false, false, false));
        }

        return data;
    }

    private String getPhaseStringProperty(HashMap<String, Object> phase,
            String property) {
        if (phase.containsKey(property) && phase.get(property) != null) {
            return (String) phase.get(property);
        }
        return null;
    }

    private int getPhaseIntegerProperty(HashMap<String, Object> phase,
            String property) {
        if (phase.containsKey(property) && phase.get(property) != null) {
            Long l = (Long) phase.get(property);
            return Math.toIntExact(l);
        }
        return 0;
    }

    private Date getPhaseDateProperty(HashMap<String, Object> phase,
            String property) {
        if (phase.containsKey(property) && phase.get(property) != null) {
            GregorianCalendar cal = (GregorianCalendar) phase.get(property);
            return cal.getTime();

        }
        return null;
    }

    /**
     * We get the first of the deliverables, as we want to be able to store more
     * than one document in the schema, but for now we cannot make it work in
     * the edition
     *
     * @param phase
     * @return
     */
    private Map<String, Object> getDeliverable(HashMap<String, Object> phase) {
        Map<String, Object> deliverable = new HashMap<String, Object>();

        if (phase.containsKey(ProjectConstants.PROJECT_PHASE_DELIVERABLES)
                && phase.get(
                        ProjectConstants.PROJECT_PHASE_DELIVERABLES) != null) {

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deliverables = (List<Map<String, Object>>) phase.get(
                    ProjectConstants.PROJECT_PHASE_DELIVERABLES);
            if (!deliverables.isEmpty()) {
                deliverable = deliverables.get(0);
            }

        }
        return deliverable;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, false, false, false);
    }

    @Override
    public RowData createRowData(String rowId, boolean isNew,
            boolean isModified, boolean isRemoved) {
        return createRowData(rowId, null, null, null, null, null, null, null,
                null, 0, null, isNew, isModified, isRemoved);
    }

    public RowData createRowData(String rowId, String parentId, String type,
            String description, String manager, Map<String, Object> deliverable,
            Date realStartDate, Date plannedEndDate, Date realEndDate,
            int progress, String comment, boolean isNew, boolean isModified,
            boolean isRemoved) {

        RowData row = new ProjectPhaseRowData(rowId, parentId, type,
                description, manager, deliverable, realStartDate,
                plannedEndDate, realEndDate, progress, comment, isNew,
                isModified, isRemoved);

        return row;
    }

}
