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

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "realUid", "parentRealUid", "title", "path" })
public class FolderInfo {

    private String parentRealUid;

    private String path;

    private String realUid;

    private String title;

    public FolderInfo() {
    }

    public FolderInfo(String realUid, String parentRealUid, String title,
            String path) {
        this.realUid = realUid;
        this.parentRealUid = parentRealUid;
        this.title = title;
        this.path = path;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
