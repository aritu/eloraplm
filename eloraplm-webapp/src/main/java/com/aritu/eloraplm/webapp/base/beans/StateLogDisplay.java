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
package com.aritu.eloraplm.webapp.base.beans;

import java.util.Date;

import com.aritu.eloraplm.core.util.StateLog;

/**
 * This class encapsulates information to display a SateLog.
 *
 * @author aritu
 *
 */
public class StateLogDisplay extends StateLog {

    protected String versionLabel;

    protected String checkinComment;

    /**
     * @param user
     * @param date
     * @param stateFrom
     * @param stateTo
     * @param transition
     * @param versionDocId
     * @param comment
     */
    public StateLogDisplay(String user, Date date, String stateFrom,
            String stateTo, String transition, String versionDocId,
            String comment) {
        super(user, date, stateFrom, stateTo, transition, versionDocId,
                comment);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param user
     * @param date
     * @param stateFrom
     * @param stateTo
     * @param transition
     * @param versionDocId
     * @param comment
     * @param versionLabel
     */
    public StateLogDisplay(String user, Date date, String stateFrom,
            String stateTo, String transition, String versionDocId,
            String comment, String versionLabel, String checkinComment) {
        super(user, date, stateFrom, stateTo, transition, versionDocId,
                comment);
        this.versionLabel = versionLabel;
        this.checkinComment = checkinComment;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getCheckinComment() {
        return checkinComment;
    }

    public void setCheckinComment(String checkinComment) {
        this.checkinComment = checkinComment;
    }

}
