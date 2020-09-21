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
package com.aritu.eloraplm.pdm.makeobsolete.treetable;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;
import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * This class encapsulates the attributes related to an Make Obsolete Node Data.
 *
 * @author aritu
 *
 */
public class MakeObsoleteNodeData extends BaseNodeData {

    private static final long serialVersionUID = 1L;

    protected DocumentModel data;

    protected DocumentModel wcDoc;

    // true, if it corresponds to the document that has initiated the make
    // obsolete process
    protected boolean isInitiatorDocument;

    protected CanMakeObsoleteResult canMakeObsoleteResult;

    protected boolean selectedToMakeObsolete;

    protected boolean mandatoryToMakeObsolete;

    /**
     * @param id
     * @param level
     */
    public MakeObsoleteNodeData(String id, int level) {
        super(id, level);
    }

    /**
     * @param id
     * @param level
     * @param data
     * @param canMakeObsoleteResult
     * @param selectedToMakeObsolete
     * @param mandatoryToMakeObsolete
     */
    public MakeObsoleteNodeData(String id, int level, DocumentModel data,
            DocumentModel wcDoc, boolean isInitiatorDocument,
            CanMakeObsoleteResult canMakeObsoleteResult,
            boolean selectedToMakeObsolete, boolean mandatoryToMakeObsolete) {
        super(id, level);
        this.data = data;
        this.wcDoc = wcDoc;
        this.isInitiatorDocument = isInitiatorDocument;
        this.canMakeObsoleteResult = canMakeObsoleteResult;
        this.selectedToMakeObsolete = selectedToMakeObsolete;
        this.mandatoryToMakeObsolete = mandatoryToMakeObsolete;
    }

    public DocumentModel getData() {
        return data;
    }

    public void setData(DocumentModel data) {
        this.data = data;
    }

    public DocumentModel getWcDoc() {
        return wcDoc;
    }

    public void setWcDoc(DocumentModel wcDoc) {
        this.wcDoc = wcDoc;
    }

    public boolean getIsInitiatorDocument() {
        return isInitiatorDocument;
    }

    public void setIsInitiatorDocument(boolean isInitiatorDocument) {
        this.isInitiatorDocument = isInitiatorDocument;
    }

    public CanMakeObsoleteResult getCanMakeObsoleteResult() {
        return canMakeObsoleteResult;
    }

    public void setCanMakeObsoleteResult(
            CanMakeObsoleteResult canMakeObsoleteResult) {
        this.canMakeObsoleteResult = canMakeObsoleteResult;
    }

    public boolean getSelectedToMakeObsolete() {
        return selectedToMakeObsolete;
    }

    public void setSelectedToMakeObsolete(boolean selectedToMakeObsolete) {
        this.selectedToMakeObsolete = selectedToMakeObsolete;
    }

    public boolean getMandatoryToMakeObsolete() {
        return mandatoryToMakeObsolete;
    }

    public void setMandatoryToMakeObsolete(boolean mandatoryToMakeObsolete) {
        this.mandatoryToMakeObsolete = mandatoryToMakeObsolete;
    }

}
