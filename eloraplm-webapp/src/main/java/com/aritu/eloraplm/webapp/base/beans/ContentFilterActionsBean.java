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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.types.SubType;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.core.DocumentContextBoundActions;

/**
 * @author aritu
 *
 *
 *
 */
@Name("contentFilterActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class ContentFilterActionsBean extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ContentFilterActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    private Map<String, String> subTypes;

    private Map<String, String> subTypeStates;

    public Map<String, String> getAuditEvents() {

        AuditLogger logger = Framework.getLocalService(AuditLogger.class);

        Set<String> events = logger.getAuditableEventNames();

        Map<String, String> eventsMap = new HashMap<String, String>();
        for (String event : events) {
            eventsMap.put(event, event);
        }

        return eventsMap;

    }

    public Map<String, String> getSubTypes() {

        // TODO helper batera pasa

        if (subTypes == null) {
            loadSubTypes();
        }

        return subTypes;

    }

    private Map<String, String> loadSubTypes() {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        String currentType = currentDoc.getType();
        TypeManager typeManager = Framework.getService(TypeManager.class);
        org.nuxeo.ecm.platform.types.Type docType = typeManager.getType(
                currentType);

        Map<String, SubType> allowedChildren = docType.getAllowedSubTypes();
        subTypes = new HashMap<String, String>();
        for (Map.Entry<String, SubType> child : allowedChildren.entrySet()) {
            String key = child.getKey();
            SubType val = child.getValue();

            if (typeManager.getType(key) != null) {
                subTypes.put(key, val.getName());
            }

        }

        return subTypes;

    }

    public Map<String, String> getSubTypeStates() {

        if (subTypeStates == null) {
            loadSubTypeStates();
        }

        return subTypeStates;
    }

    private Map<String, String> loadSubTypeStates() {

        LifeCycleService lcService = NXCore.getLifeCycleService();

        subTypeStates = new HashMap<String, String>();

        for (Map.Entry<String, String> subType : getSubTypes().entrySet()) {
            String val = subType.getValue();

            if (lcService.getLifeCycleNameFor(val) != null) {
                String lifeCycleName = lcService.getLifeCycleNameFor(val);
                LifeCycle lifeCycle = lcService.getLifeCycleByName(
                        lifeCycleName);
                List<LifeCycleState> states = (List<LifeCycleState>) lifeCycle.getStates();
                for (LifeCycleState state : states) {
                    if (!subTypeStates.containsValue(state.getName())) {
                        subTypeStates.put(state.getName(), state.getName());
                    }
                }
            }

        }

        return subTypeStates;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        subTypes = null;
        subTypeStates = null;
    }

}
