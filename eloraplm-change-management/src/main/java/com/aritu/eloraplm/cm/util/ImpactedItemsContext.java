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

import com.aritu.eloraplm.constants.CMConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */

public class ImpactedItemsContext {

    private long docsRowNumber;

    private long bomsRowNumber;

    private List<String> impactedBomsUidList;

    public ImpactedItemsContext() {
        docsRowNumber = 0;
        bomsRowNumber = 0;
        impactedBomsUidList = new ArrayList<String>();
    }

    // ---------- rowNumber (docsRowNumber, bomsRowNumber)
    public void increaseRowNumber(String itemType) {
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            docsRowNumber++;
        } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            bomsRowNumber++;
        }
    }

    public long getRowNumber(String itemType) {
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            return docsRowNumber;
        } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            return bomsRowNumber;
        }
        return -1; // this should not be possible
    }

    // ---------- impactedBomsUidList
    public void addImpactedBom(String bomUid) {
        if (!impactedBomsUidList.contains(bomUid)) {
            impactedBomsUidList.add(bomUid);
        }
    }

    public List<String> getImpactedBomsUidList() {
        return impactedBomsUidList;
    }

    public boolean existImpactedBom(String bomUid) {
        return impactedBomsUidList.contains(bomUid);
    }

}
