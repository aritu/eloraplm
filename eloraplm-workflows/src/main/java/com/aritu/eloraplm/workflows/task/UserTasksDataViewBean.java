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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author aritu
 *
 */
@Name("userTasksDataView")
@Scope(ScopeType.CONVERSATION)
public class UserTasksDataViewBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    private UserTaskLazyModel tasks;

    public UserTaskLazyModel getTasks() {
        return tasks;
    }

    public UserTasksDataViewBean() {
    }

    @Create
    public void init() {
        PageProviderService ppService = Framework.getLocalService(
                PageProviderService.class);
        tasks = new UserTaskLazyModel(documentManager, ppService,
                localeSelector.getLocaleString());
    }

    public int countTasks() {
        if (tasks != null) {
            return tasks.getRowCount();
        } else {
            return 0;
        }

    }

}
