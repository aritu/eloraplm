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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraDcDataContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Calendar creationDate;

    private Calendar modificationDate;

    private String creationUser;

    private String modificationUser;

    /**
     * @param creationDate
     * @param modificationDate
     * @param creationUser
     * @param modificationUser
     */
    public EloraDcDataContext(Date creationDate, Date modificationDate,
            String creationUser, String modificationUser) {
        super();

        // convert creationDate from Date to Calendar
        Calendar creationDateCal = Calendar.getInstance();
        creationDateCal.setTime(creationDate);
        this.creationDate = creationDateCal;

        // convert modificationDate from Date to Calendar
        Calendar modificationDateCal = Calendar.getInstance();
        modificationDateCal.setTime(modificationDate);
        this.modificationDate = modificationDateCal;

        this.creationUser = creationUser;
        this.modificationUser = modificationUser;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

    public String getCreationUser() {
        return creationUser;
    }

    public String getModificationUser() {
        return modificationUser;
    }
}
