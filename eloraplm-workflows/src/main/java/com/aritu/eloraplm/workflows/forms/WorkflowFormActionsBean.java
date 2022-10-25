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
package com.aritu.eloraplm.workflows.forms;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 * @author aritu
 *
 */
@Name("workflowFormActions")
@Scope(CONVERSATION)
public class WorkflowFormActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            WorkflowFormActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected Map<String, String> messages;

    private WorkflowFormsService wfs;

    private WorkflowFormsService getWFService() {
        if (wfs == null) {
            wfs = Framework.getService(WorkflowFormsService.class);
        }
        return wfs;
    }

    public Map<String, String> getWorkflowsMap() throws EloraException {
        return getWFService().getWorkflowsMap(documentManager);
    }

    public Map<String, String> getNodesMap(String workflowId)
            throws EloraException {
        if (workflowId != null) {

            return getWFService().getNodesMap(documentManager, workflowId);
        }

        return new HashMap<String, String>();
    }

    public Map<String, String> getFieldTypes() {
        Map<String, String> map = new HashMap<String, String>();
        for (String type : getWFService().getFieldTypes()) {
            map.put(type, "eloraplm.label.workflows.forms.types." + type);
        }
        return map;
    }

    public String getWorkflowTitle(String workflowId) throws EloraException {
        return getWorkflowsMap().get(workflowId);
    }

    public String getNodeTitle(String workflowId, String nodeId)
            throws EloraException {
        return getNodesMap(workflowId).get(nodeId);
    }

}
