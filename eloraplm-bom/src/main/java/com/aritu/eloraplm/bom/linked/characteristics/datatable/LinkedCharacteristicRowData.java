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
package com.aritu.eloraplm.bom.linked.characteristics.datatable;

import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristic;
import com.aritu.eloraplm.datatable.BaseRowData;

/**
 *
 *
 * @author aritu
 *
 */
public class LinkedCharacteristicRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private LinkedCharacteristic linkedCharacteristic;

    public LinkedCharacteristicRowData(String id,
            LinkedCharacteristic linkedCharacteristic, boolean isNew,
            boolean isRemoved, boolean isModified) {
        super(id, isNew, isRemoved, isModified);

        this.linkedCharacteristic = linkedCharacteristic;
    }

    public LinkedCharacteristic getLinkedCharacteristic() {
        return linkedCharacteristic;
    }

    public void setLinkedCharacteristic(
            LinkedCharacteristic linkedCharacteristic) {
        this.linkedCharacteristic = linkedCharacteristic;
    }

}
