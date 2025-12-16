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
package com.aritu.eloraplm.drive.webapp;

import com.aritu.eloraplm.config.util.EloraConfigRow;

/**
 *
 * @author aritu
 *
 */
public class EloraDesktopBinaryDefinition {

    protected final String binaryPackage;

    protected final String info;

    protected final String version;

    protected final String description;

    protected final String platform;

    protected final String url;

    protected final String manualUrl;

    protected final boolean isDefault;

    public EloraDesktopBinaryDefinition(EloraConfigRow row) {
        this((String) row.getProperty("package"),
                (String) row.getProperty("info"),
                (String) row.getProperty("version"),
                (String) row.getProperty("description"),
                (String) row.getProperty("platform"),
                (String) row.getProperty("url"),
                (String) row.getProperty("manualUrl"),
                (long) row.getProperty("default") == 1L ? true : false);
    }

    public EloraDesktopBinaryDefinition(String binaryPackage, String info,
            String version, String description, String platform, String url,
            String manualUrl, boolean isDefault) {
        this.binaryPackage = binaryPackage;
        this.info = info;
        this.version = version;
        this.description = description;
        this.platform = platform;
        this.url = url;
        this.manualUrl = manualUrl;
        this.isDefault = isDefault;
    }

    public String getBinaryPackage() {
        return binaryPackage;
    }

    public String getInfo() {
        return info;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getPlatform() {
        return platform;
    }

    public String getUrl() {
        return url;
    }

    public String getManualUrl() {
        return manualUrl;
    }

    public boolean getIsDefault() {
        return isDefault;
    }
}
