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
public class ItemSupplierRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private String supplier;

    private int preference;

    private String partNumber;

    public ItemSupplierRowData(String id, String supplier, int preference,
            String partNumber, boolean isNew, boolean isRemoved,
            boolean isModified) {
        super(id, isNew, isRemoved, isModified);
        this.supplier = supplier;
        this.preference = preference;
        this.partNumber = partNumber;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
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
