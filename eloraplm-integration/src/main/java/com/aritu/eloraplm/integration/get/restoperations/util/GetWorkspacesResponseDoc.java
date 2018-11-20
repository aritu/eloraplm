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

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "documents", "folders" })
public class GetWorkspacesResponseDoc {

    private String realUid;

    private String type;

    private String structureRootRealUid;

    private String title;

    private boolean isFavorite;

    private String path;

    public GetWorkspacesResponseDoc(String realUid, String type,
            String structureRootRealUid, String title, boolean isFavorite,
            String path) {
        this.realUid = realUid;
        this.type = type;
        this.structureRootRealUid = structureRootRealUid;
        this.title = title;
        this.isFavorite = isFavorite;
        this.path = path;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStructureRootRealUid() {
        return structureRootRealUid;
    }

    public void setStructureRootRealUid(String structureRootRealUid) {
        this.structureRootRealUid = structureRootRealUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
