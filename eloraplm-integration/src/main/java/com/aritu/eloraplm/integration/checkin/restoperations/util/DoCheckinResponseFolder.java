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

/**
 * @author aritu
 *
 */
public class DoCheckinResponseFolder {

    private int dbId;

    private int localId;

    private String parentRealUid;

    private String wcUid;

    public DoCheckinResponseFolder(int dbId, int localId, String parentRealUid,
            String wcUid) {
        this.dbId = dbId;
        this.localId = localId;
        this.parentRealUid = parentRealUid;
        this.wcUid = wcUid;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

}