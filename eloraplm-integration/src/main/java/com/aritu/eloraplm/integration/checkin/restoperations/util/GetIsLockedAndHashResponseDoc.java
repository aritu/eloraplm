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

import com.aritu.eloraplm.core.util.EloraLockInfo;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class GetIsLockedAndHashResponseDoc {
    private EloraLockInfo eloraLockInfo;

    private String hash;

    private String realUid;

    public GetIsLockedAndHashResponseDoc() {
        super();
    }

    public EloraLockInfo getEloraLockInfo() {
        return eloraLockInfo;
    }

    public void setEloraLockInfo(EloraLockInfo eloraLockInfo) {
        this.eloraLockInfo = eloraLockInfo;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }
}
