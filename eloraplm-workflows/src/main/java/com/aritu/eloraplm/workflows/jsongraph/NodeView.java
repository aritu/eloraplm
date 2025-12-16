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
package com.aritu.eloraplm.workflows.jsongraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;

/**
 *
 * @author aritu
 *
 */
public class NodeView {

    public int x;

    public int y;

    public boolean isStartNode;

    public boolean isEndNode;

    public String id;

    public String title;

    public String state;

    public boolean isMerge;

    public boolean isMultiTask;

    public boolean hasSubWorkflow;

    public List<String> assignees;

    public NodeView(GraphNode node, Locale locale) {
        x = Integer.parseInt((String) node.getDocument().getPropertyValue(
                GraphNode.PROP_NODE_X_COORDINATE));
        y = Integer.parseInt((String) node.getDocument().getPropertyValue(
                GraphNode.PROP_NODE_Y_COORDINATE));
        isStartNode = node.isStart();
        isEndNode = node.isStop();
        id = node.getId();
        String titleProp = (String) node.getDocument().getPropertyValue(
                GraphNode.PROP_TITLE);
        title = JsonGraphRoute.getI18nLabel(titleProp, locale);
        state = node.getDocument().getCurrentLifeCycleState();
        isMerge = node.isMerge();
        isMultiTask = node.hasMultipleTasks();
        hasSubWorkflow = node.hasSubRoute();

        assignees = new ArrayList<String>();
        try {
            List<String> assigneeIds = node.evaluateTaskAssignees();
            for (String assigneeId : assigneeIds) {
                UserNameResolverHelper unr = new UserNameResolverHelper();
                assignees.add(unr.getUserFullName(assigneeId));
            }
        } catch (DocumentRouteException e) {
            // Cannot evaluate assignees at the moment. Try using plain
            // assignees.
            assignees = node.getTaskAssignees();
        }
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isEndNode() {
        return isEndNode;
    }

    public boolean isStartNode() {
        return isStartNode;
    }

    public List<String> getAssignees() {
        return assignees;
    }
}
