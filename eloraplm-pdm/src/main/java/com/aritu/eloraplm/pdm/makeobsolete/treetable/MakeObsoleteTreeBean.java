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
package com.aritu.eloraplm.pdm.makeobsolete.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;
import com.aritu.eloraplm.pdm.makeobsolete.util.MakeObsoleteHelper;
import com.aritu.eloraplm.treetable.CoreTreeBean;

/**
 * Make Obsolete Tree.
 *
 * @author aritu
 *
 */
@Name("makeObsoleteTreeBean")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class MakeObsoleteTreeBean extends CoreTreeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            MakeObsoleteTreeBean.class);

    private MakeObsoleteNodeService nodeService;

    private MakeObsoleteService makeObsoleteService = Framework.getService(
            MakeObsoleteService.class);

    /**
     *
     */
    public MakeObsoleteTreeBean() {
        super();
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.treetable.CoreTreeBean#createRoot()
     */
    @Override
    protected void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            nodeService = new MakeObsoleteNodeService(documentManager,
                    makeObsoleteService);
            setRoot(nodeService.getRoot(getCurrentDocument()));
            setHasUnreadableNodes(false);
            setIsInvalid(false);
        } catch (DocumentNotFoundException | DocumentSecurityException e) {
            log.error(logInitMsg + e.getMessage());
            // empty root attribute and set hasUnreadableNodes attribute to true
            setRoot(new DefaultTreeNode());
            setHasUnreadableNodes(true);
            setIsInvalid(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty root attribute and set isInvalid attribute to true
            setRoot(new DefaultTreeNode());
            setIsInvalid(true);
            setHasUnreadableNodes(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public boolean canBeExecuted() {
        String logInitMsg = "[canBeExecuted] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean canBeExecuted = false;

        if (getIsInvalid() || getHasUnreadableNodes()) {
            return false;
        }

        if (nodeService != null) {
            canBeExecuted = nodeService.canBeExecuted(getRoot());
        }

        log.trace(logInitMsg + "--- EXIT --- with canBeExecuted = |"
                + canBeExecuted + "|");

        return canBeExecuted;
    }

    public void makeObsolete() throws EloraException {
        String logInitMsg = "[makeObsolete] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, CanMakeObsoleteResult> makeObsoleteResultList = new HashMap<String, CanMakeObsoleteResult>();

        try {

            makeObsoleteResultList = nodeService.makeObsolete(getRoot());

            // If there has been an error and it is not possible to make
            // obsolete the list, show the errors as faces messages.
            if (!makeObsoleteResultList.isEmpty()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.treetable.makeobsolete.cannot.make.obsolete"));

                List<String> errorMsgList = MakeObsoleteHelper.getErrorMsgListFromCannotMakeObsoleteResultList(
                        documentManager, makeObsoleteResultList, messages);

                if (errorMsgList != null && errorMsgList.size() > 0) {
                    for (Iterator<String> iterator = errorMsgList.iterator(); iterator.hasNext();) {
                        String errorMsg = iterator.next();
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                errorMsg);
                    }
                }
                throw new EloraException(
                        "Cannot make obsolete specified document list.");
            }

            navigationContext.invalidateCurrentDocument();

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void toggleNodeAsSelected(TreeNode node) {
        String logInitMsg = "[toggleNodeAsSelected] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {

            nodeService.toggleNodeAsSelected(node);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.treetable.CoreTreeBean#getRootFromFactory()
     */

    @Override
    @Factory(value = "makeObsoleteRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}
