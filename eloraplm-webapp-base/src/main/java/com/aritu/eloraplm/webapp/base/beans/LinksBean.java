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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;


/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
@Name("eloraLinks")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class LinksBean extends DocumentContextBoundActionBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LinksBean.class);

    // private static final String CHILDREN_PREDICATE_URI =
    // "http://eloraplm.aritu.com/relations/ComposedOf";

    /*
    @In(create = true)
    protected RelationActions relationActions;
    */

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(required = false)
    protected transient Principal currentUser;

    // statements lists
    protected List<String> linksWss;

    private static String getWsParentProxy(String uid) {

        String query = "SELECT " + NXQL.ECM_PARENTID + " FROM Document "
                + " WHERE " + NXQL.ECM_ISPROXY + " = 1 " + " AND "
                + NXQL.ECM_PROXY_TARGETID + " = '" + uid + "'";

        return query;
    }

    private String getCMs(String uid) {

        // TODO::: CMMetadataConstants.AFF_AFFECTED_ITEM_LIST is deprecated
        // The query must be changed.
        /* String query = "SELECT * FROM Document " + " WHERE "
                + CMMetadataConstants.AFF_AFFECTED_ITEM_LIST
                + "/*1/originItem = '" + uid + "' OR "
                + CMMetadataConstants.AFF_AFFECTED_ITEM_LIST
                + "/*1/destinationItem = '" + uid + "'";
        
        return query;*/
        return "";
    }

    public List<String> getLinksWss() {
        if (linksWss == null || linksWss.isEmpty()) {
            linksWss = getLinks();
        }
        return linksWss;
    }

    @Create
    public void create() {
        linksWss = getLinks();
    }

    /**
     * @param docM
     * @return true if it is a CAD document
     */
    public static boolean isWc(DocumentModel docM) {

        boolean isWorkingCopy = false;
        isWorkingCopy = (!docM.isProxy() && !docM.isVersion());
        return isWorkingCopy;
    }

    private List<String> getDocumentsList(String query) {
        List<String> linksUIList = new ArrayList<String>();
        DocumentModelList queryResultDocs;
        String logInitMsg = "[getDocumentsList] " + currentUser.getName();

        queryResultDocs = documentManager.query(query);
        if (queryResultDocs != null && !queryResultDocs.isEmpty()) {
            for (DocumentModel doc : queryResultDocs) {
                log.trace(logInitMsg + "doc = |" + doc.getId() + "|");
                linksUIList.add(doc.getId());
            }
        }
        if (linksUIList.isEmpty()) {
            return null;
        } else {
            return linksUIList;
        }
    }

    public List<String> getLinks() {
        DocumentModel currentDoc = getCurrentDocument();
        DocumentModel realDoc = null;
        String logInitMsg = "[getParentsWs] " + currentUser.getName();
        String query = "";
        List<String> linksUIList = new ArrayList<String>();
        List<String> partialUIList = new ArrayList<String>();

        if (!isWc(currentDoc)) {
            realDoc = null;
        } else if (isWc(currentDoc) && !currentDoc.isCheckedOut()) {
            realDoc = documentManager.getDocument(
                    documentManager.getBaseVersion(currentDoc.getRef()));
        } else if (isWc(currentDoc) && currentDoc.isCheckedOut()) {
            realDoc = null;
        }

        if (realDoc != null) {
            // WS donde figura la AV como proxy
            query = getWsParentProxy(realDoc.getId());
            log.trace(logInitMsg + "query = |" + query + "|");
            partialUIList = getDocumentsList(query);
            if (partialUIList != null) {
                linksUIList.addAll(partialUIList);
            }
            /* todo
                        // CM donde figura la AV como afectado o modificado
                        query = getCMs(realDoc.getId());
                        log.trace(logInitMsg + "query = |" + query + "|");
                        partialUIList = getDocumentsList(query);
                        if (partialUIList != null) {
                            linksUIList.addAll(partialUIList);
                        }

            */
            if (realDoc != currentDoc) {
                // WS donde figura el WC como proxy
                query = getWsParentProxy(currentDoc.getId());
                log.trace(logInitMsg + "query = |" + query + "|");
                partialUIList = getDocumentsList(query);
                if (partialUIList != null) {
                    linksUIList.addAll(partialUIList);
                }
                /*todo esperar a la estructra de cm
                                // CMs donde figura el WC como afectado o modificado
                                query = getCMs(currentDoc.getId());
                                log.trace(logInitMsg + "query = |" + query + "|");
                                partialUIList = getDocumentsList(query);
                                if (partialUIList != null) {
                                    linksUIList.addAll(partialUIList);
                                }

                                */
            }
        } else {
            query = getWsParentProxy(currentDoc.getId());
            log.trace(logInitMsg + "query = |" + query + "|");
            partialUIList = getDocumentsList(query);
            if (partialUIList != null) {
                linksUIList.addAll(partialUIList);
            }
            /* todo
                        // CMs donde figura el WC como afectado o modificado
                        query = getCMs(currentDoc.getId());
                        log.trace(logInitMsg + "query = |" + query + "|");
                        partialUIList = getDocumentsList(query);
                        if (partialUIList != null) {
                            linksUIList.addAll(partialUIList);
                        }

                        */
        }

        linksUIList.add(currentDoc.getParentRef().toString());

        if (linksUIList.isEmpty()) {
            return null;
        } else {
            return linksUIList;
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetList();
    }

    public void resetList() {
        linksWss = null;
        // childrenStatements = null;
    }

}
