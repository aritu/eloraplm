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
package com.aritu.eloraplm.securitypolicy.util;

import java.util.Date;

public class SecurityInfo {

    private Integer levelId;

    private Date levelSetDate;

    private String levelSetBy;

    public SecurityInfo(Integer levelId, Date levelSetDate, String levelSetBy) {
        setLevelId(levelId);
        this.setLevelSetDate(levelSetDate);
        this.setLevelSetBy(levelSetBy);
    }

    public Integer getLevelId() {
        return levelId;
    }

    public void setLevelId(Integer levelId) {
        this.levelId = levelId;
    }

    public Date getLevelSetDate() {
        return levelSetDate;
    }

    public void setLevelSetDate(Date levelSetDate) {
        this.levelSetDate = levelSetDate;
    }

    public String getLevelSetBy() {
        return levelSetBy;
    }

    public void setLevelSetBy(String levelSetBy) {
        this.levelSetBy = levelSetBy;
    }
}