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
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;

/**
 * @author aritu
 *
 */
@Name("integrationActions")
@Scope(CONVERSATION)
public class IntegrationActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pluginInstallerUrl;

    private boolean isPluginInstallerWebUrl = false;

    private String pluginInstallerVersion;

    private String pluginInstallerName = "Aritu.Plugin.Installer.exe";

    public String getPluginInstallerUrl() {
        if (pluginInstallerUrl == null) {
            pluginInstallerUrl = EloraConfig.generalConfigMap.get(
                    EloraConfigConstants.KEY_INTEGRATOR_BINARY_URL);
            if (pluginInstallerUrl.startsWith("http://")
                    || pluginInstallerUrl.startsWith("https://")) {
                isPluginInstallerWebUrl = true;
            }
        }
        return pluginInstallerUrl;
    }

    public boolean getIsPluginInstallerWebUrl() {
        if (pluginInstallerUrl == null) {
            getPluginInstallerUrl();
        }
        return isPluginInstallerWebUrl;
    }

    public String getPluginInstallerVersion() {
        if (pluginInstallerVersion == null) {
            pluginInstallerVersion = EloraConfig.generalConfigMap.get(
                    EloraConfigConstants.KEY_INTEGRATOR_BINARY_VERSION);
        }
        return pluginInstallerVersion;
    }

    public String getPluginInstallerName() {
        return pluginInstallerName;
    }
}
