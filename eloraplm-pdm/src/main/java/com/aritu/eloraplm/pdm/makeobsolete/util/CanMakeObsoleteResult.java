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
package com.aritu.eloraplm.pdm.makeobsolete.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the concept of Can Make Obsolete. It stores if a
 * document can be made obsolete or not, and if not, the reason.
 *
 * @author aritu
 *
 */
public class CanMakeObsoleteResult {

    private boolean canMakeObsolete;

    // This list contains the identifiers of the documents that avoid making
    // obsolete a document.
    private List<String> incompatibleRelatedDocIds;

    private String cannotMakeObsoleteReasonMsg;

    private String cannotMakeObsoleteReasonMsgParam;

    // If nothing is specified, initialize to false
    public CanMakeObsoleteResult() {
        this(false);
    }

    /**
     * @param canMakeObsolete
     */
    public CanMakeObsoleteResult(boolean canMakeObsolete) {
        super();
        this.canMakeObsolete = canMakeObsolete;
        incompatibleRelatedDocIds = new ArrayList<String>();
        cannotMakeObsoleteReasonMsg = "";
        cannotMakeObsoleteReasonMsgParam = "";
    }

    public CanMakeObsoleteResult(boolean canMakeObsolete,
            List<String> incompatibleRelatedDocIds) {
        this(canMakeObsolete);
        this.incompatibleRelatedDocIds = incompatibleRelatedDocIds;
    }

    public CanMakeObsoleteResult(boolean canMakeObsolete,
            String cannotMakeObsoleteReasonMsg) {
        this(canMakeObsolete);
        this.cannotMakeObsoleteReasonMsg = cannotMakeObsoleteReasonMsg;
    }

    public CanMakeObsoleteResult(boolean canMakeObsolete,
            String cannotMakeObsoleteReasonMsg,
            String cannotMakeObsoleteReasonMsgParam) {
        this(canMakeObsolete, cannotMakeObsoleteReasonMsg);
        this.cannotMakeObsoleteReasonMsgParam = cannotMakeObsoleteReasonMsgParam;
    }

    // getters and setters
    public boolean getCanMakeObsolete() {
        return canMakeObsolete;
    }

    public void setCanMakeObsolete(boolean canMakeObsolete) {
        this.canMakeObsolete = canMakeObsolete;
    }

    public List<String> getIncompatibleRelatedDocIds() {
        return incompatibleRelatedDocIds;
    }

    public void setIncompatibleRelatedDocIds(
            List<String> docIdsForWhichCannotMakeObsolete) {
        incompatibleRelatedDocIds = docIdsForWhichCannotMakeObsolete;
    }

    public String getCannotMakeObsoleteReasonMsg() {
        return cannotMakeObsoleteReasonMsg;
    }

    public void setCannotMakeObsoleteReasonMsg(
            String cannotMakeObsoleteReasonMsg) {
        this.cannotMakeObsoleteReasonMsg = cannotMakeObsoleteReasonMsg;
    }

    public String getCannotMakeObsoleteReasonMsgParam() {
        return cannotMakeObsoleteReasonMsgParam;
    }

    public void setCannotMakeObsoleteReasonMsgParam(
            String cannotMakeObsoleteReasonMsgParam) {
        this.cannotMakeObsoleteReasonMsgParam = cannotMakeObsoleteReasonMsgParam;
    }

}
