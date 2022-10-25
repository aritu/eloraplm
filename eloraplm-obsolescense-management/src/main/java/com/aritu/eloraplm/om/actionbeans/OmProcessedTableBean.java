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
package com.aritu.eloraplm.om.actionbeans;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
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
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;

import com.aritu.eloraplm.constants.OmEventNames;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.datatable.DocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.om.util.OmHelper;

/**
 *
 * @author aritu
 *
 */

@Name("omProcessedTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class OmProcessedTableBean extends DocBasedTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            OmProcessedTableBean.class);

    private boolean reloadData = false;

    public OmProcessedTableBean() {
        super();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");

            setData(OmHelper.getOmProcessDocList(documentManager,
                    getCurrentDocument(),
                    OmMetadataConstants.OM_PROCESSED_DOC_LIST));

            log.trace(logInitMsg + "Table created.");

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    public List<RowData> getData() {
        return super.getData();
    }

    @Override
    @Factory(value = "omProcessedRows", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Observer(value = { OmEventNames.OM_PROCESSED_LIST_UPDATED })
    @BypassInterceptors
    public void markToBeReloaded() {
        reloadData = true;
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        if (reloadData) {
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: processed list updated.");
        }

        return doc;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        super.resetBeanCache(newCurrentDocumentModel);
        reloadData = false;
    }

}
