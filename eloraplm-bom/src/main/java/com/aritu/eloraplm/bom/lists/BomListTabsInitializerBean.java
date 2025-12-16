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
package com.aritu.eloraplm.bom.lists;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.ecm.platform.actions.ActionPropertiesDescriptor;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.platform.actions.DefaultActionFilter;
import org.nuxeo.ecm.platform.actions.FilterRule;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.BomListsConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.config.util.EloraPropertiesHelper;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Initializes BOM List tabs when the user starts session.
 *
 * @author aritu
 *
 */
@Name("bomListTabsInitializer")
@Scope(ScopeType.SESSION)
@Startup
public class BomListTabsInitializerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Log log = LogFactory.getLog(
            BomListTabsInitializerBean.class);

    public static BomListTabsInitializerBean instance() {
        if (!Contexts.isSessionContextActive()) {
            return null;
        }
        return (BomListTabsInitializerBean) Component.getInstance(
                BomListTabsInitializerBean.class, ScopeType.SESSION);
    }

    @Observer(EventNames.USER_SESSION_STARTED)
    public void loadBomListTabs() {
        // TODO Lortzen bada ebenturen batekin edo botoi batekin eguneratzea
        // BomListak, hobeto BomListsConfig sortu eta hor jartzea konfigurazioa
        // estatikoki (klase bat sortuta hobeto)
        // TODO Saioa amaitu/hasi arte ez dira bom listen aldaketak kontutan
        // hartuko. Ebenturen batekin eguneratu beharko zen...
        // Gutxi gora-behera horrela deituko zaio:
        // BomListTabsInitializer bomListTabsInit =
        // BomListTabsInitializer.instance();
        // if (bomListTabsInit == null) {
        // log.warn("BomListTabsInitializer not available. Can't load BOM List
        // tabs);
        // return;
        // }
        // bomListTabsInit.loadBomListTabs();

        // TODO ActionManager izan beharko zen, baina interfazeak ez dauka
        // registerContribution metodorik
        ActionService actionService = (ActionService) Framework.getLocalService(
                ActionManager.class);
        String bomCompositionListTabPrefix = "TAB_BOM_COMPOSITION_";
        String bomWhereUsedListTabPrefix = "TAB_BOM_WHERE_USED_";
        try {
            EloraConfigTable bomListsTable = BomListsConfigHelper.getBomLists(
                    false);
            if (!bomListsTable.isEmpty()) {
                for (EloraConfigRow row : bomListsTable.getValues()) {
                    if (EloraPropertiesHelper.areBomListsExternal()) {
                        createBomListTabs(actionService, row,
                                bomCompositionListTabPrefix,
                                "/incl/tabs/external_bom_composition_list.xhtml",
                                EloraRelationConstants.DIRECTION_COMPOSITION);
                        createBomListTabs(actionService, row,
                                bomWhereUsedListTabPrefix,
                                "/incl/tabs/external_bom_where_used_list.xhtml",
                                EloraRelationConstants.DIRECTION_WHERE_USED);
                    } else {
                        createBomListTabs(actionService, row,
                                bomCompositionListTabPrefix,
                                "/incl/tabs/bom_composition_list.xhtml",
                                EloraRelationConstants.DIRECTION_COMPOSITION);
                        createBomListTabs(actionService, row,
                                bomWhereUsedListTabPrefix,
                                "/incl/tabs/bom_where_used_list.xhtml",
                                EloraRelationConstants.DIRECTION_WHERE_USED);
                    }
                }
                log.trace("BOM list tabs created.");
            }
        } catch (EloraException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // TODO logs
        }
    }

    private void createBomListTabs(ActionService actionService,
            EloraConfigRow row, String bomListTabPrefix, String tabLinkUrl,
            String treeDirection) {
        String bomListId = (String) row.getProperty("id");
        String tabId = bomListTabPrefix + bomListId;
        int tabOrder = 0;
        long ordering = (long) row.getProperty("ordering");
        // We want the list tabs to load after the EBOM tab
        tabOrder = (int) (ordering + 10);

        if (actionService.getAction(tabId) == null) {
            String[] category = new String[1];
            category[0] = bomListTabPrefix + "sub_tab";
            Action action = new Action(tabId, category);
            action.setLabel((String) row.getProperty("label"));
            action.setType("rest_document_link");
            action.setOrder(tabOrder);
            action.setLink(tabLinkUrl);
            action.setEnabled(true);

            ActionFilter[] filters = createFilter(tabId, bomListId,
                    treeDirection);
            action.setFilters(filters);

            /*
            List<String> filterIds = createFilter(tabId, bomListId,
                    treeDirection, actionService);

            action.setFilterIds(filterIds);
            */

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("ajaxSupport", "true");
            ActionPropertiesDescriptor apd = new ActionPropertiesDescriptor();
            apd.setProperties(properties);
            action.setPropertiesDescriptor(apd);

            actionService.registerContribution(action, "actions", null);
        }
    }

    private ActionFilter[] createFilter(String tabId, String bomListId,
            String treeDirection) {
        boolean grant = true;
        String[] permissions = null;
        String[] facets = null;

        String[] conditions = new String[] {
                "#{bomList.getIsSubTabVisible(currentDocument.elo.reference, '"
                        + bomListId + "', '" + treeDirection + "')}" };

        // String[] conditions = new String[] {
        // "#{currentDocument.isCheckedOut()}" };

        String[] types = null;
        String[] schemas = null;
        FilterRule rule = new FilterRule(grant, permissions, facets, conditions,
                types, schemas);
        FilterRule[] rules = new FilterRule[] { rule };

        DefaultActionFilter filter = new DefaultActionFilter();
        filter.setId("showMbomTab_" + tabId);
        filter.setRules(rules);

        /*
        actionService.registerContribution(filter, "filters", null);
        List<String> filterIds = new ArrayList<String>();
        filterIds.add(filter.getId());

        return filterIds;
        */
        ActionFilter[] filters = new ActionFilter[] { filter };
        return filters;
    }
}
