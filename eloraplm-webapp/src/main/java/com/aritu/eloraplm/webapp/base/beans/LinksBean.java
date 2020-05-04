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
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Bean for retrieving Elora links.
 *
 * @author aritu
 *
 */
@Name("eloraLinks")
@Scope(EVENT)
public class LinksBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LinksBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected List<String> links;

    public List<String> getLinks() {
        /*String logInitMsg = "[getLinks] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");*/

        if (links == null || links.isEmpty()) {
            links = getLinksUidList();
        }

        // log.trace(logInitMsg + "--- EXIT ---");
        return links;
    }

    protected List<String> getLinksUidList() {
        String logInitMsg = "[getLinksUidList] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> linksUIList = new ArrayList<String>();

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
                // Retrieve the proxies related to the current document (AV)
                query = EloraQueryFactory.getDocProxyParentsQuery(
                        currentDoc.getType(), currentDoc.getId());
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);

            } else {
                // Retrieve the proxies related to the WC and all its versions
                List<String> versionsUids = new ArrayList<String>();
                versionsUids.add(currentDoc.getId());

                List<DocumentModel> versions = documentManager.getVersions(
                        currentDoc.getRef());
                if (versions != null && versions.size() > 0) {
                    for (int i = 0; i < versions.size(); i++) {
                        DocumentModel versionDoc = versions.get(i);
                        versionsUids.add(versionDoc.getId());
                    }
                }
                query = EloraQueryFactory.getDocProxyParentsQuery(
                        currentDoc.getType(), versionsUids);
                EloraQueryHelper.executeQueryAndAppendResultUidList(query,
                        uidCollection, documentManager);
            }

            // Add current document's parent
            uidCollection.add(currentDoc.getParentRef().toString());

            if (!uidCollection.isEmpty()) {
                linksUIList.addAll(uidCollection);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT --- with linksUIList.size = |"
                + linksUIList.size() + "|");

        if (linksUIList.isEmpty()) {
            return null;
        } else {
            return linksUIList;
        }

    }

}
