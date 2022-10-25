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
package com.aritu.eloraplm.bom.lists.util;

import com.aritu.eloraplm.datatable.BaseRowData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;

public class BomListComparisonRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private RelationNodeData firstItemNodeData;

    private RelationNodeData secondItemNodeData;

    private int level;

    private boolean multipleFirstItems;

    private boolean multipleSecondItems;

    private boolean diffQuantity;

    private boolean diffOrdering;

    public BomListComparisonRowData(String id,
            RelationNodeData firstItemNodeData,
            RelationNodeData secondItemNodeData, int level, boolean isNew,
            boolean isModified, boolean isRemoved) {

        super(id, isNew, isModified, isRemoved);

        this.firstItemNodeData = firstItemNodeData;
        this.secondItemNodeData = secondItemNodeData;
        this.level = level;
        diffQuantity = false;
        diffOrdering = false;
        multipleFirstItems = false;
        multipleSecondItems = false;
    }

    public RelationNodeData getFirstItemNodeData() {
        return firstItemNodeData;
    }

    public void setFirstItemNodeData(RelationNodeData firstItemNodeData) {
        this.firstItemNodeData = firstItemNodeData;
    }

    public RelationNodeData getSecondItemNodeData() {
        return secondItemNodeData;
    }

    public void setSecondItemNodeData(RelationNodeData secondItemNodeData) {
        this.secondItemNodeData = secondItemNodeData;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean getMultipleFirstItems() {
        return multipleFirstItems;
    }

    public void setMultipleFirstItems(boolean multipleFirstItems) {
        this.multipleFirstItems = multipleFirstItems;
    }

    public boolean getMultipleSecondItems() {
        return multipleSecondItems;
    }

    public void setMultipleSecondItems(boolean multipleSecondItems) {
        this.multipleSecondItems = multipleSecondItems;
    }

    public boolean getDiffQuantity() {
        return diffQuantity;
    }

    public void setDiffQuantity(boolean diffQuantity) {
        this.diffQuantity = diffQuantity;
    }

    public boolean getDiffOrdering() {
        return diffOrdering;
    }

    public void setDiffOrdering(boolean diffOrdering) {
        this.diffOrdering = diffOrdering;
    }

}