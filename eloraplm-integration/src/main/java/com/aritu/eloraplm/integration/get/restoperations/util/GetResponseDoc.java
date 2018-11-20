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

import com.aritu.eloraplm.integration.util.UidResponseDoc;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class GetResponseDoc extends UidResponseDoc {

    private String title;

    private String type;

    private String parentRealUid;

    private String path;

    public GetResponseDoc(String wcUid, String realUid, String title,
            String type, String parentRealUid, String path) {
        super(wcUid, realUid);
        this.title = title;
        this.type = type;
        this.parentRealUid = parentRealUid;
        this.path = path;
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

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentWcUid) {
        parentRealUid = parentWcUid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}