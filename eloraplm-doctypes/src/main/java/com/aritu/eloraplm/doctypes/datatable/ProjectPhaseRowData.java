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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.aritu.eloraplm.datatable.BaseRowData;

/**
 *
 * @author aritu
 *
 */
public class ProjectPhaseRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private String parentId;

    private String type;

    private String title;

    private String description;

    private String manager;

    private List<Map<String, Object>> deliverables;

    private Date realStartDate;

    private Date plannedEndDate;

    private Date realEndDate;

    private int progress;

    private String comment;

    private String result;

    private boolean obsolete;

    public ProjectPhaseRowData(String id, String parentId, String type,
            String title, String description, String manager,
            List<Map<String, Object>> deliverables, Date realStartDate,
            Date plannedEndDate, Date realEndDate, int progress, String comment,
            String result, boolean obsolete, boolean isNew, boolean isModified,
            boolean isRemoved) {
        super(id, isNew, isModified, isRemoved);

        this.parentId = parentId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.manager = manager;
        this.deliverables = deliverables;
        this.realStartDate = realStartDate;
        this.plannedEndDate = plannedEndDate;
        this.realEndDate = realEndDate;
        this.progress = progress;
        this.comment = comment;
        this.result = result;
        this.obsolete = obsolete;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Date getRealEndDate() {
        return realEndDate;
    }

    public void setRealEndDate(Date realEndDate) {
        this.realEndDate = realEndDate;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean getObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

}
