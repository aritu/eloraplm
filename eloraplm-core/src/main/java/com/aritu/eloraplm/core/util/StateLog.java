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

/**
 * This class encapsulates State Log fields.
 *
 * @author aritu
 *
 */
/**
 * This class encapsulates SateLog information.
 *
 * @author aritu
 *
 */
public class StateLog {

    protected String user;

    protected Date date;

    protected String stateFrom;

    protected String stateTo;

    protected String transition;

    protected String versionDocId;

    protected String comment;

    /**
     * @param user
     * @param date
     * @param stateFrom
     * @param stateTo
     * @param transition
     * @param versionDocId
     * @param comment
     */
    public StateLog(String user, Date date, String stateFrom, String stateTo,
            String transition, String versionDocId, String comment) {
        super();
        this.user = user;
        this.date = date;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.transition = transition;
        this.versionDocId = versionDocId;
        this.comment = comment;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStateFrom() {
        return stateFrom;
    }

    public void setStateFrom(String stateFrom) {
        this.stateFrom = stateFrom;
    }

    public String getStateTo() {
        return stateTo;
    }

    public void setStateTo(String stateTo) {
        this.stateTo = stateTo;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public String getVersionDocId() {
        return versionDocId;
    }

    public void setVersionDocId(String versionDocId) {
        this.versionDocId = versionDocId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
