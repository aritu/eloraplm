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
package com.aritu.eloraplm.integration.checkin.restoperations.util;

import java.util.ArrayList;
import java.util.List;

import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;

/**
 * @author aritu
 *
 */
public class TryCheckinResponseFolder {

    private int localId;

    private String wcUid;

    private String type;

    private String title;

    private String result;

    private List<ValidationErrorItem> errorList;

    public TryCheckinResponseFolder(int localId, String wcUid, String type,
            String title) {
        this.localId = localId;
        this.wcUid = wcUid;
        this.type = type;
        this.title = title;
        // Initialize error list
        errorList = new ArrayList<ValidationErrorItem>();
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<ValidationErrorItem> getErrorList() {
        return errorList;
    }

    public void addError(String field, String message) {
        errorList.add(new ValidationErrorItem(field, message));
    }

    public void addErrorList(List<ValidationErrorItem> errorList) {
        this.errorList.addAll(errorList);
    }

}
