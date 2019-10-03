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
package com.aritu.eloraplm.integration.checkin.restoperations.util;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "initialLifeCycleState", "modified", "lastContributor",
        "versionLabel" })
public class ForceMetadataInfo {

    private String initialLifeCycleState;

    private Date modified;

    private String lastContributor;

    private String versionLabel;

    public ForceMetadataInfo() {
    }

    public ForceMetadataInfo(String initialLifeCycleState, Date modified,
            String lastContributor, String versionLabel) {
        this.initialLifeCycleState = initialLifeCycleState;
        this.modified = modified;
        this.lastContributor = lastContributor;
        this.versionLabel = versionLabel;
    }

    public String getInitialLifeCycleState() {
        return initialLifeCycleState;
    }

    public void setInitialLifeCycleState(String initialLifeCycleState) {
        this.initialLifeCycleState = initialLifeCycleState;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

}
