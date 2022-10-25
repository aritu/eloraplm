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
package com.aritu.eloraplm.om.archiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 * @author aritu
 *
 */
public class OmArchiverConditions {

    private static final Log log = LogFactory.getLog(
            OmArchiverConditions.class);

    public static boolean isOmProcessInAllowedState(DocumentModel workspace) {

        if (workspace == null) {
            return false;
        }

        CoreSession session = workspace.getCoreSession();

        if (workspace.getCurrentLifeCycleState().equals("completed")
                || workspace.getCurrentLifeCycleState().equals("rejected")) {
            return true;
        } else {
            String logInitMsg = "[isFullECOArchived] ["
                    + session.getPrincipal().getName() + "] ";
            log.error(logInitMsg
                    + "Could not archive the OM Process as it does not meet the required conditions."
                    + " OM Process' lifecycle state must be completed or rejected in order to archive it.");
            return false;
        }
    }

}
