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

import java.util.ArrayList;
import java.util.List;

import com.aritu.eloraplm.cm.ImpactedItem;

/**
 * This class encapsulates the attributes of a Related Item.
 *
 * @author aritu
 *
 */
public class ImpactedItemWithChildren {

    private ImpactedItem impactedItem;

    private List<String> children = new ArrayList<String>();

    public ImpactedItemWithChildren(ImpactedItem impactedItem,
            List<String> children) {
        this.impactedItem = impactedItem;
        if (children != null) {
            this.children = children;
        }
    }

    public ImpactedItem getImpactedItem() {
        return impactedItem;
    }

    public List<String> getChildren() {
        return children;
    }

}
