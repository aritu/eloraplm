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
package com.aritu.eloraplm.om.util;

import java.util.ArrayList;
import java.util.List;

import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;

public class ObsoleteProcessResult {

    private boolean allProcessed;

    private List<String> newImpactedDocList;

    private List<String> missingImpactedDocList;

    private CanMakeObsoleteResult obsoleteResult;

    public ObsoleteProcessResult(boolean allProcessed) {
        setAllProcessed(allProcessed);
        setNewImpactedDocList(new ArrayList<String>());
        setMissingImpactedDocList(new ArrayList<String>());
    }

    public boolean getAllProcessed() {
        return allProcessed;
    }

    public void setAllProcessed(boolean allProcessed) {
        this.allProcessed = allProcessed;
    }

    public List<String> getNewImpactedDocList() {
        return newImpactedDocList;
    }

    public void setNewImpactedDocList(List<String> newImpactedDocList) {
        this.newImpactedDocList = newImpactedDocList;
    }

    public void addToNewImpactedDocList(List<String> newImpactedDocList) {
        this.newImpactedDocList.addAll(newImpactedDocList);
    }

    public List<String> getMissingImpactedDocList() {
        return missingImpactedDocList;
    }

    public void setMissingImpactedDocList(List<String> missingImpactedDocList) {
        this.missingImpactedDocList = missingImpactedDocList;
    }

    public void addToMissingImpactedDocList(
            List<String> missingImpactedDocList) {
        this.missingImpactedDocList.addAll(missingImpactedDocList);
    }

    public CanMakeObsoleteResult getObsoleteResult() {
        return obsoleteResult;
    }

    public void setObsoleteResult(CanMakeObsoleteResult obsoleteResult) {
        this.obsoleteResult = obsoleteResult;
    }

}
