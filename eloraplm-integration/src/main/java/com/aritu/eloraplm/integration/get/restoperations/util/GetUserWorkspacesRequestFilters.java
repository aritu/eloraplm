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
package com.aritu.eloraplm.integration.get.restoperations.util;

import java.util.List;

/**
 * @author aritu
 *
 */
public class GetUserWorkspacesRequestFilters {

    private String lifeCycleState;

    private String type;

    private String structureRoot;

    private boolean onlyFavorite;

    private List<String> uidList;

    private boolean includeArchived;

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public void setLifeCycleState(String lifeCycleState) {
        this.lifeCycleState = lifeCycleState;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStructureRoot() {
        return structureRoot;
    }

    public void setStructureRoot(String structureRoot) {
        this.structureRoot = structureRoot;
    }

    public boolean isOnlyFavorite() {
        return onlyFavorite;
    }

    public void setOnlyFavorite(boolean onlyFavorite) {
        this.onlyFavorite = onlyFavorite;
    }

    public List<String> getUidList() {
        return uidList;
    }

    public void setUidList(List<String> uidList) {
        this.uidList = uidList;
    }

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public void setIncludeArchived(boolean includeArchived) {
        this.includeArchived = includeArchived;
    }

}
