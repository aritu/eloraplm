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
package com.aritu.eloraplm.cm.archiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 *
 * @author aritu
 *
 */
public class CmArchiverConditions {

    private static final Log log = LogFactory.getLog(
            CmArchiverConditions.class);

    public static boolean isFullECOArchived(DocumentModel workspace)
            throws EloraException {

        CoreSession session = workspace.getCoreSession();

        String logInitMsg = "[isFullECOArchived] ["
                + session.getPrincipal().getName() + "] ";

        long count = 0;
        count += getCountNotManaged(session, workspace.getId(),
                CMConstants.ITEM_TYPE_BOM, CMConstants.ITEM_CLASS_MODIFIED);
        count += getCountNotManaged(session, workspace.getId(),
                CMConstants.ITEM_TYPE_DOC, CMConstants.ITEM_CLASS_MODIFIED);
        count += getCountNotManaged(session, workspace.getId(),
                CMConstants.ITEM_TYPE_BOM, CMConstants.ITEM_CLASS_IMPACTED);
        count += getCountNotManaged(session, workspace.getId(),
                CMConstants.ITEM_TYPE_DOC, CMConstants.ITEM_CLASS_IMPACTED);

        log.trace(logInitMsg + "Count of nodes not managed in ECO |"
                + workspace.getId() + "| : " + count);
        if (count > 0) {
            log.error(logInitMsg
                    + "Could not archive the ECO as it does not meet the required conditions."
                    + " All nodes in ECO (both modified and impacted) must be managed in order to archive it.");
        }

        return count == 0;
    }

    private static long getCountNotManaged(CoreSession session,
            String workspaceId, String itemType, String itemClass)
            throws EloraException {

        String query = CMQueryFactory.getCountItemsByItemClassTypeAndManagedQuery(
                workspaceId, itemType, itemClass, 0);
        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

}
