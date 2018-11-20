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
import java.util.List;

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
import com.aritu.eloraplm.webapp.base.beans.LinksBean;

/**
 * Bean for retrieving Elora Change Management links.
 *
 * @author aritu
 *
 */
@Name("eloraCmLinks")
@Scope(EVENT)
public class CMLinksBean extends LinksBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CMLinksBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @Override
    protected List<String> getLinksUidList() {
        String logInitMsg = "[getLinksUidList] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> linksUIList = new ArrayList<String>();

        try {

            linksUIList = super.getLinksUidList();

            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc.isProxy()) {
                currentDoc = documentManager.getSourceDocument(
                        currentDoc.getRef());
            }

            String query = "";

            // if current document is a AV
            if (currentDoc.isImmutable()) {

                // Retrieve the CM processes where current document (AV) is
                // defined as origin or destination of a modified item
                query = CMHelper.getProcessesByModifiedItemOriginQuery(
                        currentDoc);
                executeQueryAndAddResultToUidList(query, linksUIList);

                query = CMHelper.getProcessesByModifiedItemDestinationQuery(
                        currentDoc);
                executeQueryAndAddResultToUidList(query, linksUIList);

            } else {

                // Retrieve the CM processes where current document (WC) is
                // defined as origin or destination of a modified item
                query = CMHelper.getProcessesByModifiedItemOriginWcQuery(
                        currentDoc);
                executeQueryAndAddResultToUidList(query, linksUIList);

                query = CMHelper.getProcessesByModifiedItemDestinationWcQuery(
                        currentDoc);
                executeQueryAndAddResultToUidList(query, linksUIList);
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
