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
package com.aritu.eloraplm.cm.beans;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Bean for retrieving Elora Change Management content.
 *
 * @author aritu
 *
 */
@Name("eloraCmContent")
@Scope(EVENT)
public class CMContentBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CMContentBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected List<String> uidList;

    public List<String> getUidList() {
        String logInitMsg = "[getUidList] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (uidList == null || uidList.isEmpty()) {
            uidList = retrieveUidList();
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return uidList;
    }

    protected List<String> retrieveUidList() {
        String logInitMsg = "[retrieveUidList] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> uids = new ArrayList<String>();

        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc.isProxy()) {
                currentDoc = documentManager.getSourceDocument(
                        currentDoc.getRef());
            }

            String query = "";
            Set<String> uidCollection = new HashSet<String>();

            // if current document is a AV
            if (currentDoc.isImmutable()) {

                // Retrieve the CM processes where current document (AV) is
                // defined as origin or destination of a modified item
                query = CMHelper.getProcessesByModifiedItemOriginQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                query = CMHelper.getProcessesByModifiedItemDestinationQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                // Retrieve the CM processes where current document (AV) is
                // defined as origin or destination of a impacted item
                query = CMHelper.getProcessesByImpactedItemOriginQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                query = CMHelper.getProcessesByImpactedItemDestinationQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

            } else {

                // Retrieve the CM processes where current document (WC) is
                // defined as origin or destination of a modified item
                query = CMHelper.getProcessesByModifiedItemOriginWcQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                query = CMHelper.getProcessesByModifiedItemDestinationWcQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                // Retrieve the CM processes where current document (WC) is
                // defined as origin or destination of a impacted item
                query = CMHelper.getProcessesByImpactedItemOriginWcQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

                query = CMHelper.getProcessesByImpactedItemDestinationWcQuery(
                        currentDoc);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);
            }

            if (!uidCollection.isEmpty()) {
                uids.addAll(uidCollection);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT --- with uidList.size = |"
                + uids.size() + "|");

        if (uids.isEmpty()) {
            return null;
        } else {
            return uids;
        }
    }
}
