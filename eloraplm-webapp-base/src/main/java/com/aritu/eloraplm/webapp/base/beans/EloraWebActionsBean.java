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

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * @author aritu
 *
 */
@Name("eloraWebActions")
@Scope(CONVERSATION)
public class EloraWebActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    /**
     * @param expression
     * @return
     */
    public Boolean evaluateBooleanExpression(String expression) {
        FacesContext context = FacesContext.getCurrentInstance();
        Boolean result = context.getApplication().evaluateExpressionGet(
                context, expression, Boolean.class);

        return result;
    }

    /**
     * @param directoryName
     * @param entryId
     * @return
     */
    public static DocumentModel getDirectoryEntry(String directoryName,
            String entryId) {
        if (entryId == null) {
            return null;
        }
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        try (Session session = dirService.open(directoryName)) {
            return session.getEntry(entryId);
        }
    }

    /**
     * @param doc
     * @return
     */
    public boolean isEditable(DocumentModel doc) {
        NuxeoPrincipal user = (NuxeoPrincipal) documentManager.getPrincipal();
        if (!doc.isVersion()
                && !doc.isProxy()
                && doc.isLocked()
                && documentManager.hasPermission(doc.getRef(),
                        SecurityConstants.WRITE)
                && user.getName().equals(
                        documentManager.getLockInfo(doc.getRef()).getOwner())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isInAWorkspace() {

        DocumentModel superSpace = documentManager.getSuperSpace(navigationContext.getCurrentDocument());

        if (superSpace.getType().equals(NuxeoDoctypeConstants.WORKSPACE)) {
            return true;
        }

        return false;
    }
}
