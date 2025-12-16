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
package com.aritu.eloraplm.bom.datatable;

import com.aritu.eloraplm.datatable.BaseRowData;

/**
 *
 * @author aritu
 *
 */
public class ItemOemRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private String manufacturer;

    private int preference;

    private String partNumber;

    public ItemOemRowData(String id, String manufacturer, int preference,
            String partNumber, boolean isNew, boolean isModified,
            boolean isRemoved) {
        super(id, isNew, isModified, isRemoved);
        this.manufacturer = manufacturer;
        this.preference = preference;
        this.partNumber = partNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public int getPreference() {
        return preference;
    }

    public void setPreference(int preference) {
        this.preference = preference;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

}
