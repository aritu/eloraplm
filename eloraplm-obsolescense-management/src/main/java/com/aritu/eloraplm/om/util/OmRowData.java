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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.versioning.VersioningService;

import com.aritu.eloraplm.datatable.BaseRowData;
import com.aritu.eloraplm.datatable.RowData;

/**
 *
 * @author aritu
 *
 */
public class OmRowData extends BaseRowData implements RowData {

    private static final long serialVersionUID = 1L;

    private String uid;

    private String versionableId;

    private boolean isAnarchic;

    private String originState;

    private String destinationState;

    private String classification;

    private DocumentModel data;

    private String versionNumber;

    private List<String> anarchicTopDocs;

    private String errorMsg;

    private String errorMsgParam;

    private boolean isOk;

    private boolean isProcessed;

    public OmRowData(CoreSession session, String id, String uid,
            boolean isAnarchic, String originState, String destinationState,
            String classification, boolean isOk, String errorMsg,
            String errorMsgParam) {

        this(session, id, uid, isAnarchic, originState, destinationState,
                classification, new ArrayList<String>(), isOk, errorMsg,
                errorMsgParam);
    }

    public OmRowData(CoreSession session, String id, String uid,
            boolean isAnarchic, String originState, String destinationState,
            String classification, List<String> anarchicTopDocs, boolean isOk,
            String errorMsg, String errorMsgParam) {

        super(id);
        this.uid = uid;
        this.isAnarchic = isAnarchic;
        this.originState = originState;
        this.destinationState = destinationState;
        this.classification = classification;
        this.anarchicTopDocs = anarchicTopDocs;
        this.isOk = isOk;
        this.errorMsg = errorMsg;
        this.errorMsgParam = errorMsgParam;

        data = session.getDocument(new IdRef(uid));
        versionNumber = data.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP) + "."
                + data.getPropertyValue(VersioningService.MINOR_VERSION_PROP);
        versionableId = data.getVersionSeriesId();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVersionableId() {
        return versionableId;
    }

    public void setVersionableId(String versionableId) {
        this.versionableId = versionableId;
    }

    public boolean getIsAnarchic() {
        return isAnarchic;
    }

    public void setIsAnarchic(boolean isAnarchic) {
        this.isAnarchic = isAnarchic;
    }

    public String getOriginState() {
        return originState;
    }

    public void setOriginState(String originState) {
        this.originState = originState;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public void setDestinationState(String destinationState) {
        this.destinationState = destinationState;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public DocumentModel getData() {
        return data;
    }

    public void setData(DocumentModel data) {
        this.data = data;
    }

    public boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public List<String> getAnarchicTopDocs() {
        return anarchicTopDocs;
    }

    public void addAnarchicTopDoc(String docUid) {
        anarchicTopDocs.add(docUid);
    }

    public boolean getIsOk() {
        return isOk;
    }

    public void setIsOk(boolean isOk) {
        this.isOk = isOk;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsgParam() {
        return errorMsgParam;
    }

    public void setErrorMsgParams(String errorMsgParam) {
        this.errorMsgParam = errorMsgParam;
    }
}
