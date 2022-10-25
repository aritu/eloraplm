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
import java.util.HashMap;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author aritu
 *
 */
@Name("eloraPopupHelper")
@Scope(EVENT)
public class EloraPopupHelperBean implements Serializable {

    private static final Log log = LogFactory.getLog(EloraPopupHelperBean.class);

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    protected transient EloraFavoritesActionBean eloraFavoritesActions;

    @WebRemote
    public String getNavigationURLOnPopupdocInNewTab(String docId) {
        Map<String, String> params = new HashMap<String, String>();
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        if (doc != null) {
            return DocumentModelFunctions.documentUrl(null, doc, null, params,
                    true, getRequest());
        }
        return null;
    }

    private HttpServletRequest getRequest() {
        HttpServletRequest request = ServletContexts.instance().getRequest();
        if (request != null) {
            return request;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return (HttpServletRequest) context.getExternalContext().getRequest();
        }
        return null;
    }

    @WebRemote
    public void removeDocumentFromFavorites(String docId) {

        String logInitMsg = "[removeDocumentFromFavorites] ";

        log.trace(logInitMsg + "--- ENTER --- docId = |" + docId + "|");

        DocumentModel doc = documentManager.getDocument(new IdRef(docId));

        eloraFavoritesActions.removeDocumentFromFavorites(doc);

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
