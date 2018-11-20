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
package com.aritu.eloraplm.bom.characteristics.treetable;

import com.aritu.eloraplm.bom.characteristics.BomCharacteristic;
import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * @author aritu
 *
 */

public class BomCharacteristicsNodeData extends BaseNodeData {

    private static final long serialVersionUID = 1L;

    protected BomCharacteristic bomCharacteristic;

    protected String classificationLabel;

    protected String bomCharacteristicTypeConstraints;

    // Constructors
    public BomCharacteristicsNodeData(String id, int level) {

        this(id, level, false, false, false, null, null, null);
    }

    public BomCharacteristicsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified,
            BomCharacteristic bomCharacteristic, String classificationLabel,
            String bomCharacteristicTypeConstraints) {

        super(id, level, isNew, isRemoved, isModified);

        this.bomCharacteristic = bomCharacteristic;
        this.classificationLabel = classificationLabel;
        this.bomCharacteristicTypeConstraints = bomCharacteristicTypeConstraints;

    }

    public BomCharacteristic getBomCharacteristic() {
        return bomCharacteristic;
    }

    public void setBomCharacteristic(BomCharacteristic bomCharacteristic) {
        this.bomCharacteristic = bomCharacteristic;
    }

    public String getClassificationLabel() {
        return classificationLabel;
    }

    public void setClassificationLabel(String classificationLabel) {
        this.classificationLabel = classificationLabel;
    }

    public String getBomCharacteristicTypeConstraints() {
        return bomCharacteristicTypeConstraints;
    }

    public void setBomCharacteristicTypeConstraints(
            String bomCharacteristicTypeConstraints) {
        this.bomCharacteristicTypeConstraints = bomCharacteristicTypeConstraints;
    }

}
