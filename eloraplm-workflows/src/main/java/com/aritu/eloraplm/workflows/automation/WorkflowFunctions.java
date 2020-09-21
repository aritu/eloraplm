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
package com.aritu.eloraplm.workflows.automation;

import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.TaskInfo;

/**
 *
 * @author aritu
 *
 */
public class WorkflowFunctions extends CoreFunctions {

    public Object getTaskInfoValue(TaskInfo taskInfo, String property) {

        if (taskInfo != null) {
            switch (property) {
            case "status":
                return taskInfo.getStatus();
            case "actor":
                return taskInfo.getActor();
            case "comment":
                return taskInfo.getComment();
            case "taskDocId":
                return taskInfo.getTaskDocId();
            case "node":
                return taskInfo.getNode();
            }
        }

        return null;
    }

    public String concatenate(String... values) {

        String text = "";

        for (String value : values) {
            if (value == null) {
                continue;
            }

            text += value;
        }

        return text;
    }

}
