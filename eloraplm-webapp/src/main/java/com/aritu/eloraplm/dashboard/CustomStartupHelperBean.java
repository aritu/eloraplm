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
package com.aritu.eloraplm.dashboard;

import static org.jboss.seam.annotations.Install.DEPLOYMENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webapp.helpers.StartupHelper;

@Name("startupHelper")
@Scope(ScopeType.SESSION)
@Install(precedence = DEPLOYMENT)
public class CustomStartupHelperBean extends StartupHelper {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CustomStartupHelperBean.class);

    @Override
    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public String initDomainAndFindStartupPage(String domainTitle,
            String viewId) {
        String logInitMsg = "[initDomainAndFindStartupPage] ";
        try {
            log.trace(logInitMsg + "Getting start page...");
            String result = super.initDomainAndFindStartupPage(domainTitle,
                    viewId);
            NuxeoPrincipal nxPrincipal = ((NuxeoPrincipal) documentManager.getPrincipal());
            logInitMsg += "[" + nxPrincipal.getName() + "] ";
            log.trace(logInitMsg + "Obtained default Nuxeo start page.");
            if (nxPrincipal.isAdministrator()) {
                log.trace(logInitMsg
                        + "User is admin. Returning default Nuxeo start page...");
                return result;
            } else {
                log.trace(logInitMsg
                        + "User is not admin, obtaining dashboard page...");
                return dashboardNavigationHelper.navigateToDashboard();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled error on startup: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            return null;
        }
    }
}