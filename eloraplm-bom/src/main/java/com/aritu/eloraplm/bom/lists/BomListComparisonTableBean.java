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
package com.aritu.eloraplm.bom.lists;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.datatable.DocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;

/**
 *
 * @author aritu
 *
 */
@Name("bomListComparisonTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomListComparisonTableBean extends DocBasedTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomListComparisonTableBean.class);

    private String comparisonFirstDocUid;

    private String comparisonSecondDocUid;

    private String bomListTypeFirstDoc;

    private String bomListTypeSecondDoc;

    private Boolean differentStructure;

    public String getComparisonFirstDocUid() {
        return comparisonFirstDocUid;
    }

    public void setComparisonFirstDocUid(String comparisonFirstDocUid) {
        this.comparisonFirstDocUid = comparisonFirstDocUid;
    }

    public String getComparisonSecondDocUid() {
        return comparisonSecondDocUid;
    }

    public void setComparisonSecondDocUid(String comparisonSecondDocUid) {
        this.comparisonSecondDocUid = comparisonSecondDocUid;
    }

    public String getBomListTypeFirstDoc() {
        return bomListTypeFirstDoc;
    }

    public void setBomListTypeFirstDoc(String bomListTypeFirstDoc) {
        this.bomListTypeFirstDoc = bomListTypeFirstDoc;
    }

    public String getBomListTypeSecondDoc() {
        return bomListTypeSecondDoc;
    }

    public void setBomListTypeSecondDoc(String bomListTypeSecondDoc) {
        this.bomListTypeSecondDoc = bomListTypeSecondDoc;
    }

    public Boolean getDifferentStructure() {
        return differentStructure;
    }

    public void setDifferentStructure(Boolean differentStructure) {
        this.differentStructure = differentStructure;
    }

    public BomListComparisonTableBean() {
        tableService = new BomListComparisonTableServiceImpl();
    }

    @Observer(value = { EloraEventNames.ELORA_BOM_LIST_SUBTAB_CHANGED_EVENT })
    @BypassInterceptors
    public void updateComparisonFields(String subtabId, String docId) {
        setBomListTypeFirstDoc(BomListConstants.BOM_LIST_EBOM);
        setBomListTypeSecondDoc(subtabId);
        setComparisonFirstDocUid(docId);
        setComparisonSecondDocUid(docId);
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            differentStructure = false;
            List<RowData> rowDataList = new ArrayList<RowData>();

            if (comparisonFirstDocUid != null
                    && comparisonSecondDocUid != null) {

                log.trace(logInitMsg + "Creating table...");

                DocumentModel comparisonFirstDoc = documentManager.getDocument(
                        new IdRef(comparisonFirstDocUid));
                DocumentModel comparisonSecondDoc = documentManager.getDocument(
                        new IdRef(comparisonSecondDocUid));

                BomListComparisonTableServiceImpl bomListCompTableServ = (BomListComparisonTableServiceImpl) tableService;

                rowDataList = bomListCompTableServ.getData(comparisonFirstDoc,
                        comparisonSecondDoc, bomListTypeFirstDoc,
                        bomListTypeSecondDoc, documentManager);

                differentStructure = bomListCompTableServ.getDifferentStructure();

                setData(rowDataList);
            } else {
                setData(rowDataList);
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        comparisonFirstDocUid = null;
        comparisonSecondDocUid = null;
        bomListTypeFirstDoc = null;
        bomListTypeSecondDoc = null;
        differentStructure = false;
        createData();

        // super.resetBeanCache(newCurrentDocumentModel);
    }

    @Override
    @Factory(value = "bomListComparisonDataFactory", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

}
