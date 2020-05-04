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

package com.aritu.eloraplm.bom.autostructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.bom.autostructure.util.ItemStructureData;
import com.aritu.eloraplm.bom.autostructure.util.ItemStructureData.DocItemRelation;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.exceptions.CompositionWithMultipleVersionsException;

@Name("bomStructureAction")
@Scope(ScopeType.PAGE)
public class BomStructureActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomStructureActionBean.class);

    private DocumentModel currentDoc;

    Map<String, List<DocumentModel>> itemList;

    private ItemStructureData itemStructureData;

    private List<DocItemRelation> selectedDocItemRelations;

    private boolean hasDirector;

    private boolean hasWrontTypes;

    private boolean hasMissingItems;

    private List<DocumentModel> missingItems;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    // @Observer(value = { PdmEventNames.PDM_CHECKED_OUT_EVENT }, create = true)
    public void calculateStructure() {
        String logInitMsg = "[calculateStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        DocumentModel currentDoc = getCurrentDocument();
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            selectedDocItemRelations = new ArrayList<DocItemRelation>();

            itemStructureData = new ItemStructureData(currentDoc,
                    documentManager);
        } catch (CompositionWithMultipleVersionsException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.createStructure.error.multiple.versions"),
                    e.getDocument().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    e.getDocument().getTitle());
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.createStructure.error"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }
    }

    public void updateStructure() {
        String logInitMsg = "[updateStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (itemStructureData.hasDirector()) {
                ItemStructureUpdater.updateStructure(itemStructureData,
                        selectedDocItemRelations, eloraDocumentRelationManager,
                        documentManager);

                if (itemStructureData.isStructureUpdated()) {
                    // Events.instance().raiseEvent(
                    // AutostructureEventNames.AUTOSTRUCTURE_UPDATED_EVENT);
                }
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.createStructure.success.updated"));
            }
        } catch (CompositionWithMultipleVersionsException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.createStructure.error.multiple.versions"),
                    e.getDocument().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    e.getDocument().getTitle());
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.autostructure.error"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }
    }

    private DocumentModel getCurrentDocument() {
        currentDoc = navigationContext.getCurrentDocument();
        return currentDoc.isProxy()
                ? documentManager.getWorkingCopy(currentDoc.getRef())
                : currentDoc;
    }

    public ItemStructureData getItemStructureData() {
        return itemStructureData;
    }

    public boolean getHasStructure() {
        return itemStructureData.hasStructure();
    }

    public boolean getHasMultipleItems() {
        return itemStructureData.getDocItemRelationList().size() > 0;
    }

    public List<DocItemRelation> getDocItemRelationList() {
        List<DocItemRelation> docItemRelationList = new ArrayList<DocItemRelation>();
        if (itemStructureData != null && itemStructureData.hasDirector()) {
            docItemRelationList = itemStructureData.getDocItemRelationList();
        }
        return docItemRelationList;
    }

    public List<DocItemRelation> getSelectedDocItemRelations() {
        if (selectedDocItemRelations != null) {
            return selectedDocItemRelations;
        } else {
            return new ArrayList<DocItemRelation>();
        }
    }

    public void setSelectedDocItemRelations(
            List<DocItemRelation> selectedDocItemRelations) {
        this.selectedDocItemRelations = selectedDocItemRelations;
    }

    public boolean getHasDirector() {
        return itemStructureData.hasDirector();
    }

    public boolean getHasWrongTypes() {
        return itemStructureData.hasWrongTypes();
    }

    public List<DocumentModel> getWrongTypeItems() {
        if (itemStructureData != null) {
            return itemStructureData.getWrongTypeItemList();
        } else {
            return new ArrayList<DocumentModel>();
        }
    }

    public boolean getHasMissingItems() {
        return itemStructureData.hasMissingItems();
    }

    public List<DocumentModel> getMissingItems() {
        if (itemStructureData != null) {
            return itemStructureData.getMissingItemList();
        } else {
            return new ArrayList<DocumentModel>();
        }
    }

}
