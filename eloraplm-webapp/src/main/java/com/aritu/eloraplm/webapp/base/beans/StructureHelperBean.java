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
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.webapp.duplicating.DuplicatingActionsBean;

/**
 * @author aritu
 *
 */
@Name("structureHelper")
@Scope(EVENT)
public class StructureHelperBean implements Serializable {

    private static final Log log = LogFactory.getLog(StructureHelperBean.class);

    private static final long serialVersionUID = 1L;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    protected transient EloraFavoritesActionBean eloraFavoritesActions;

    @In(create = true)
    protected transient DuplicatingActionsBean duplicatingActions;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected Map<String, String> messages;

    public Map<String, String> getVaultFoldersMap() {
        String logInitMsg = "[getVaultFoldersMap] ["
                + documentManager.getPrincipal().getName() + "] ";

        Map<String, String> vaultFoldersMap = new LinkedHashMap<String, String>();

        try {

            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            vaultFoldersMap = EloraStructureHelper.getVaultFoldersMap(
                    currentDocument);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.getVaultFolders"));
        }

        return vaultFoldersMap;
    }

    public String getVaultFolderInitialValue() {
        String vaultFolder = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (currentDocument.hasFacet(EloraFacetConstants.FACET_VAULT_FOLDER)) {
            vaultFolder = currentDocument.getId();
        }
        return vaultFolder;
    }

}
