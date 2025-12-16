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

package com.aritu.eloraplm.securitypolicy;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.securitypolicy.api.SecurityConfigService;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;
import com.aritu.eloraplm.securitypolicy.util.SecurityInfo;

/**
 *
 * @author aritu
 *
 */
@Name("securityPolicyBean")
@Scope(ScopeType.EVENT)
public class SecurityPolicyBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In
    protected transient NavigationContext navigationContext;

    private static final Log log = LogFactory.getLog(SecurityPolicyBean.class);

    private Integer levelId;

    private SecurityConfigService scs;

    private SecurityConfigService getSecurityConfigService() {
        if (scs == null) {
            scs = Framework.getService(SecurityConfigService.class);
        }
        return scs;
    }

    public String getLevelLabel(Integer levelId) {
        return getSecurityConfigService().getLevelLabel(levelId);
    }

    public Integer getLevelId() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        Long levelId = (Long) currentDoc.getPropertyValue(
                SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL);
        return levelId.intValue();
    }

    public void setLevelId(Integer levelId) {
        this.levelId = levelId;
    }

    public Map<Integer, String> getSecurityLevels() {
        return getSecurityConfigService().getLevelsMap();
    }

    public String getLevelColor(Integer levelId) {
        return getSecurityConfigService().getLevelColor(levelId);
    }

    public void save() throws Exception {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            SecurityInfo securityInfo = new SecurityInfo(levelId, new Date(),
                    documentManager.getPrincipal().getName());

            getSecurityConfigService().updateSecurityInfo(currentDoc,
                    securityInfo);

            navigationContext.invalidateCurrentDocument();

            log.trace(logInitMsg + "Security level |" + levelId
                    + "| saved for document |" + currentDoc.getId() + "|.");

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.securityPolicy.error"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }

    }

}
