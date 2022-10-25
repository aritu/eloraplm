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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.OmEventNames;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.datatable.DocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.om.calculations.OmProcessCalculations;
import com.aritu.eloraplm.om.util.ObsoleteProcessResult;
import com.aritu.eloraplm.om.util.OmHelper;

/**
 *
 * @author aritu
 *
 */

@Name("omImpactedTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class OmImpactedTableBean extends DocBasedTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(OmImpactedTableBean.class);

    private boolean reloadData = false;

    public OmImpactedTableBean() {
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
                    OmMetadataConstants.OM_IMPACTED_DOC_LIST));

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
    @Factory(value = "omImpactedRows", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Observer(value = { OmEventNames.OM_IMPACT_LIST_UPDATED })
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
            log.trace(
                    logInitMsg + "Document invalidated: impact list updated.");
        }

        return doc;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        super.resetBeanCache(newCurrentDocumentModel);
        reloadData = false;
    }

    public void calculateImpactedDocs() {
        String logInitMsg = "[calculateImpactedDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Calculating impact for process |"
                + getCurrentDocument().getId() + "|...");

        DocumentModel sourceDoc = getObsoleteSourceDoc(getCurrentDocument());
        try {
            OmProcessCalculations ompc = new OmProcessCalculations(
                    documentManager, getCurrentDocument(), sourceDoc);
            ompc.calculateAndSaveImpact();

            Events.instance().raiseEvent(OmEventNames.OM_IMPACT_LIST_UPDATED);

            EloraEventHelper.fireEvent(OmEventNames.OM_IMPACT_CALCULATED,
                    getCurrentDocument());

        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.om.documentUnreadable"));
        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.calculateImpact"));

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.calculateImpact"));

        }
        log.trace(logInitMsg + "Impact calculated.");
    }

    public void executeProcess() {
        String logInitMsg = "[executeProcess] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Executing obsolescense process |"
                + getCurrentDocument().getId() + "|...");

        DocumentModel sourceDoc = getObsoleteSourceDoc(getCurrentDocument());

        try {
            List<String> selectedAnarchics = getSelectedRows().stream().map(
                    RowData::getId).collect(Collectors.toList());

            if (!getCurrentDocument().isLocked()) {
                EloraDocumentHelper.lockDocument(getCurrentDocument());
            }

            OmProcessCalculations ompc = new OmProcessCalculations(
                    documentManager, getCurrentDocument(), sourceDoc);
            ObsoleteProcessResult result = ompc.processImpactedDocs(
                    selectedAnarchics);

            if (result.getNewImpactedDocList().size() > 0
                    || result.getMissingImpactedDocList().size() > 0) {

                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.messages.error.om.impactListChanged"));
            } else if (result.getObsoleteResult() != null
                    && !result.getObsoleteResult().getCanMakeObsolete()) {

                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.messages.error.om.executeProcess"));
            } else {

                if (!getCurrentDocument().getCurrentLifeCycleState().equals(
                        EloraLifeCycleConstants.WORKING)) {
                    getCurrentDocument().followTransition(
                            EloraLifeCycleConstants.TRANS_TO_WORKING);
                }
            }

            Events.instance().raiseEvent(OmEventNames.OM_IMPACT_LIST_UPDATED);
            Events.instance().raiseEvent(
                    OmEventNames.OM_PROCESSED_LIST_UPDATED);

            EloraEventHelper.fireEvent(OmEventNames.OM_IMPACT_PROCESSED,
                    getCurrentDocument());

        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.om.documentUnreadable"));
        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.executeProcess"));

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.executeProcess"));

        }
        log.trace(logInitMsg + "Process executed.");
    }

    private DocumentModel getObsoleteSourceDoc(DocumentModel currentDocument) {
        String sourceDocRealUid = (String) currentDocument.getPropertyValue(
                OmMetadataConstants.OM_SOURCE_DOC_REAL_UID);
        if (sourceDocRealUid == null) {
            return null;
        }

        DocumentModel sourceDoc = documentManager.getDocument(
                new IdRef(sourceDocRealUid));
        return sourceDoc;
    }

}
