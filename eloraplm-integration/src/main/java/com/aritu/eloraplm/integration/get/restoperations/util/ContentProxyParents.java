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

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ContentProxyParents {

    private String wcUid;

    private List<String> parentRealUids;

    public ContentProxyParents(String wcUid) {
        this.wcUid = wcUid;
        parentRealUids = new ArrayList<String>();
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public List<String> getParentRealUids() {
        return parentRealUids;
    }

    public void addParentRealUid(String parentRealUid) {
        parentRealUids.add(parentRealUid);
    }

    public boolean hasMultipleParents() {
        return parentRealUids != null && parentRealUids.size() > 1;
    }

}