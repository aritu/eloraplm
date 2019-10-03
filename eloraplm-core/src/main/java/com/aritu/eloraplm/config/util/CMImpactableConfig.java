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
package com.aritu.eloraplm.config.util;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CMImpactableConfig {

    // String id;

    String docType;

    String action;

    boolean isImpactable;

    boolean includeInImpactMatrixDefaultValue;

    /**
     * @param docType
     * @param action
     * @param isImpactable
     * @param includeInImpactMatrixDefaultValue
     */
    public CMImpactableConfig(String docType, String action,
            boolean isImpactable, boolean includeInImpactMatrixDefaultValue) {
        super();
        // this.id = id;
        this.docType = docType;
        this.action = action;
        this.isImpactable = isImpactable;
        this.includeInImpactMatrixDefaultValue = includeInImpactMatrixDefaultValue;
    }

    /*public String getId() {
        return id;
    }
    */

    public String getDocType() {
        return docType;
    }

    public String getAction() {
        return action;
    }

    public boolean getIsImpactable() {
        return isImpactable;
    }

    public boolean getIncludeInImpactMatrixDefaultValue() {
        return includeInImpactMatrixDefaultValue;
    }

}
