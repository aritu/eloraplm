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
package com.aritu.eloraplm.integration.restoperations.util;

/**
 * Lifecycle state info
 *
 * @author aritu
 *
 */
public class EloraLifecycleStateInfo {

    private String id;

    private String label;

    private String shortLabel;

    private String color;

    private boolean isFinalState;

    public EloraLifecycleStateInfo(String id, String label, String shortLabel,
            String color, boolean isFinalState) {
        super();
        this.id = id;
        this.label = label;
        this.shortLabel = shortLabel;
        this.color = color;
        this.isFinalState = isFinalState;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isFinalState() {
        return isFinalState;
    }

    public void setFinalState(boolean isFinalState) {
        this.isFinalState = isFinalState;
    }

}
