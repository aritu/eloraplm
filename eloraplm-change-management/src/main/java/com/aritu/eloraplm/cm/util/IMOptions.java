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
package com.aritu.eloraplm.cm.util;

import com.aritu.eloraplm.constants.CMConstants;

/**
 * This class encapsulates the attributes of a Related Item.
 *
 * @author aritu
 *
 */
public class IMOptions {

    private String calculationMethod = CMConstants.IM_CALCULATION_METHOD_AUTOMATIC;

    private boolean createItemIM = true;

    private boolean createDocIM = true;

    private boolean lockImpacted = true;

    private boolean overwriteImpacted = false;

    private String overwriteOption;

    public IMOptions(String calculationMethod, boolean createItemIM,
            boolean createDocIM, boolean lockImpacted,
            boolean overwriteImpacted, String overwriteOption) {
        this.calculationMethod = calculationMethod;
        this.createItemIM = createItemIM;
        this.createDocIM = createDocIM;
        this.lockImpacted = lockImpacted;
        this.overwriteImpacted = overwriteImpacted;
        this.overwriteOption = overwriteOption;
    }

    public Boolean getCreateItemIM() {
        return createItemIM;
    }

    public void setCreateItemIM(Boolean createItemIM) {
        this.createItemIM = createItemIM;
    }

    public Boolean getCreateDocIM() {
        return createDocIM;
    }

    public void setCreateDocIM(Boolean createDocIM) {
        this.createDocIM = createDocIM;
    }

    public Boolean getLockImpacted() {
        return lockImpacted;
    }

    public void setLockImpacted(Boolean lockImpacted) {
        this.lockImpacted = lockImpacted;
    }

    public Boolean getOverwriteImpacted() {
        return overwriteImpacted;
    }

    public void setOverwriteImpacted(Boolean overwriteImpacted) {
        this.overwriteImpacted = overwriteImpacted;
    }

    public String getOverwriteOption() {
        return overwriteOption;
    }

    public void setOverwriteOption(String overwriteOption) {
        this.overwriteOption = overwriteOption;
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

}
