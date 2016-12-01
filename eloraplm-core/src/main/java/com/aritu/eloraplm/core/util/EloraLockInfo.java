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

package com.aritu.eloraplm.core.util;

import java.util.Date;

public class EloraLockInfo {
    private boolean isLocked;
    private String lockedBy;
    private Date lockCreated;
    private boolean isLockable;

    public EloraLockInfo(boolean isLocked, String lockedBy, Date lockCreated, boolean isLockable) {
        this.isLocked = isLocked;
        this.lockedBy = lockedBy;
        this.lockCreated = lockCreated;
        this.isLockable = isLockable;
    }
    public boolean getIsLocked() {
        return isLocked;
    }
    public String getLockedBy() {
        return lockedBy;
    }
    public Date getLockCreated() {
        return lockCreated;
    }
    public boolean getIsLockable() {
        return isLockable;
    }
}
