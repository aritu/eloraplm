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
package com.aritu.eloraplm.workflows.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.core.service.DocumentTaskProvider;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class UserTaskLazyModel extends LazyDataModel<Task> {

    private static final long serialVersionUID = 1L;

    private CoreSession session;

    private PageProviderService ppService;

    private PageProvider<DocumentModel> pp;

    private String locale;

    // private Map<String, SortMeta> sortBy;

    // private Map<String, FilterMeta> filterBy;

    private List<String> actors;

    public UserTaskLazyModel(CoreSession session, PageProviderService ppService,
            String locale) {
        this.session = session;
        this.ppService = ppService;
        this.locale = locale;
        actors = new ArrayList<String>();
        if (session != null) {
            String username = session.getPrincipal().getName();
            actors.add(username);
        }
    }

    @Override
    public int getRowCount() {
        if (pp != null) {
            return (int) pp.getResultsCount();
        } else {
            return 0;
        }
    }

    @Override
    public List<Task> load(int first, int pageSize,
            Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        if (session != null) {
            // this.sortBy = sortBy;
            // this.filterBy = filterBy;
            queryPP(first, pageSize);
            int count = (int) pp.getResultsCount();
            setRowCount(count);
            return DocumentTaskProvider.wrapDocModelInTask(pp.getCurrentPage());
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void queryPP(int first, int pageSize) {

        // for(Map.Entry<String, SortMeta> sb : sortBy.entrySet()) // TODO
        SortInfo si = new SortInfo(TaskConstants.TASK_DUE_DATE_PROPERTY_NAME,
                false);
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(si);
        Long currentPage = (long) (first / pageSize);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        props.put(
                CoreQueryDocumentPageProvider.USE_UNRESTRICTED_SESSION_PROPERTY,
                Boolean.TRUE);
        props.put("locale", locale);

        pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                TaskQueryConstant.GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS_PP,
                sortInfos, (long) pageSize, currentPage, props, actors, actors);
    }
}
