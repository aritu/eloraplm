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
package com.aritu.eloraplm.integration.util;

import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ItemInfo {

    private String reference;

    private String title;

    private String type;

    private VersionInfo currentVersionInfo;

    private String currentLifeCycleState;

    public ItemInfo() {
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public VersionInfo getCurrentVersionInfo() {
        return currentVersionInfo;
    }

    public void setCurrentVersionInfo(VersionInfo currentVersionInfo) {
        this.currentVersionInfo = currentVersionInfo;
    }

    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    public void setCurrentLifeCycleState(String currentLifeCycleState) {
        this.currentLifeCycleState = currentLifeCycleState;
    }
}
