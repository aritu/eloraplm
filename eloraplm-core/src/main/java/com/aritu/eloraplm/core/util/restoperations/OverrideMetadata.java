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
package com.aritu.eloraplm.core.util.restoperations;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author aritu
 *
 */
public class OverrideMetadata {

    private String property;

    private Serializable value;

    public OverrideMetadata(String property, Serializable value) {
        this.property = property;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public Serializable getValue() {
        // TODO REMOVE WHEN FIXED - mapeoko datuei mota jartzen zaienean ez da
        // beharko
        // TODO IMMn bueltatzen dena UUIDak dira. Zer egin? Oraingoz, ez denez
        // balio hori erabiltzen, dagoen bezala uzten da.
        if (value instanceof Collection<?>) {
            @SuppressWarnings("unchecked")
            List<String> valueList = (List<String>) value;
            return String.join("|", valueList);
        } else {
            return value;
        }
    }

    public void setValue(Serializable value) {
        this.value = value;
    }
}
