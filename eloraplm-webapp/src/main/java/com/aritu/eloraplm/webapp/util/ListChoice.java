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
package com.aritu.eloraplm.webapp.util;

import java.util.List;

/**
 *
 * @author aritu
 *
 */
public class ListChoice {

    private String choiceId;

    private int order;

    private List<LocalizedLabel> labels;

    public String getChoiceId() {
        return choiceId;
    }

    public void setChoiceId(String choiceId) {
        this.choiceId = choiceId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<LocalizedLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalizedLabel> labels) {
        this.labels = labels;
    }

    public ListChoice(String choiceId, int order, List<LocalizedLabel> labels) {
        this.choiceId = choiceId;
        this.order = order;
        this.labels = labels;
    }

}
