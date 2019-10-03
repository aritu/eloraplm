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
package com.aritu.eloraplm.qm.beans;

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

import com.aritu.eloraplm.qm.util.QMQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Bean for retrieving Elora Quality Management content.
 *
 * @author aritu
 *
 */
@Name("eloraQmContent")
@Scope(EVENT)
public class QMContentBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(QMContentBean.class);

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

            // Retrieve the QM processes where current document is
            // defined as subject
            query = QMQueryFactory.getProcessesBySubjectQuery(
                    currentDoc.getId());
            uidCollection = EloraQueryHelper.executeQueryAndGetResultUidList(
                    query, documentManager);

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
