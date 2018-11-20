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
package com.aritu.eloraplm.integration.restoperations.util;

import com.aritu.eloraplm.core.util.EloraLockInfo;

/**
 * @author aritu
 *
 */
public class EloraLockUnlockResponseDoc {

    private String wcUid;

    private String realUid;

    private EloraLockInfo eloraLockInfo;

    private String result;

    private String error;

    public EloraLockUnlockResponseDoc(String wcUid, String realUid) {
        this.wcUid = wcUid;
        this.realUid = realUid;
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public EloraLockInfo getEloraLockInfo() {
        return eloraLockInfo;
    }

    public void setEloraLockInfo(EloraLockInfo eloraLockInfo) {
        this.eloraLockInfo = eloraLockInfo;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
