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
package com.aritu.eloraplm.bom.datatable;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.component.datatable.DataTable;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.datatable.EditableDocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 *
 * @author aritu
 *
 */
@Name("itemCustomersTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ItemCustomersTableBean extends EditableDocBasedTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ItemCustomersTableBean.class);

    @In(create = true)
    private EloraDocumentRelationManager eloraDocumentRelationManager;

    private VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    public ItemCustomersTableBean() {
        tableService = new ItemCustomersTableServiceImpl();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(getCurrentDocument()));
            setIsDirty(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    @Factory(value = "itemCustomersData", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (getIsDirty()) {
            try {
                List<String> customers = new ArrayList<String>();

                DocumentModel doc = getCurrentDocument();
                if (doc.isProxy()) {
                    doc = documentManager.getWorkingCopy(doc.getRef());
                }

                for (RowData row : getData()) {
                    if (!row.getIsRemoved()) {
                        customers.add(row.getId());
                    } else {
                        ItemCustomerRowData custRow = (ItemCustomerRowData) row;
                        removeRelations(custRow.getCustomerProductDoc(), doc);
                    }
                }

                doc.refresh();
                if (doc.isCheckedOut()) {
                    doc.setPropertyValue(
                            EloraMetadataConstants.ELORA_CUST_CUSTOMERS,
                            (Serializable) customers);

                    documentManager.saveDocument(doc);
                } else {
                    DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                            doc);
                    if (baseDoc == null) {
                        throw new EloraException("The document |" + doc.getId()
                                + "| has no base version.");
                    }

                    EloraDocumentHelper.disableVersioningDocument(baseDoc);
                    baseDoc.setPropertyValue(
                            EloraMetadataConstants.ELORA_CUST_CUSTOMERS,
                            (Serializable) customers);
                    baseDoc = documentManager.saveDocument(baseDoc);

                    EloraDocumentHelper.restoreToVersion(doc.getRef(),
                            baseDoc.getRef(), true, true, documentManager);
                }

                createData();

            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.item.customers.save"));
            }
            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.item.customers.save"));
        }
    }

    public String createAndRelateCustomerProduct(String customerId,
            DataTable table, ItemCustomerRowData row, String rowIndex)
            throws EloraException {
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        try {
            DocumentModel custProdDoc = createCustomerProduct(currentDoc,
                    customerId);

            // Check in new document
            EloraDocumentHelper.setupCheckIn(versionLabelService, custProdDoc,
                    "Created from Product: " + currentDoc.getTitle());

            custProdDoc = documentManager.saveDocument(custProdDoc);
            addRelations(currentDoc, custProdDoc);
            row.setCustomerProductDoc(custProdDoc);
            refreshRow(table, rowIndex);

        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }

        return null;
    }

    private DocumentModel createCustomerProduct(DocumentModel doc,
            String customerId) throws EloraException {
        String eloraRootFolderId = EloraStructureHelper.getEloraRootFolderUid(
                doc, documentManager);

        String targetFolderPath = EloraStructureHelper.getPathByType(
                new IdRef(eloraRootFolderId),
                EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT,
                EloraDoctypeConstants.STRUCTURE_EBOM, documentManager);

        String custName = EloraConfigHelper.getCustomerConfig(customerId);
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String pathCustName = pss.generatePathSegment(custName);
        DocumentModel custProdDoc = documentManager.createDocumentModel(
                targetFolderPath, pathCustName,
                EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT);

        custProdDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                custName);
        custProdDoc.setPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE,
                doc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE));
        custProdDoc.setPropertyValue(
                EloraMetadataConstants.ELORA_BOMCUSTPROD_CUSTOMER, customerId);

        return documentManager.createDocument(custProdDoc);
    }

    private void addRelations(DocumentModel currentDoc,
            DocumentModel custProdDoc) {
        // Add inverse relation taking custProdDoc as subject, wc -> wc
        eloraDocumentRelationManager.addRelation(documentManager, custProdDoc,
                currentDoc, EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT, "",
                "1");

        // TODO Base?
        // We don't use eloraDocumentHelper.getLatestVersion because in this
        // case this returns the same and maybe it is more efficient
        DocumentModel custProdDocLastVersion = documentManager.getLastDocumentVersion(
                custProdDoc.getRef());
        DocumentModel currentDocLastVersion = documentManager.getLastDocumentVersion(
                currentDoc.getRef());
        if (currentDocLastVersion != null) {
            // Add inverse relation taking custProdDocLastVersion as subject,
            // av->av
            eloraDocumentRelationManager.addRelation(documentManager,
                    custProdDocLastVersion, currentDocLastVersion,
                    EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT, "", "1");
        }
    }

    private void removeRelations(DocumentModel custProduct,
            DocumentModel item) {

        if (custProduct != null && item != null) {
            eloraDocumentRelationManager.softDeleteRelation(documentManager,
                    custProduct,
                    EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT, item);
            // TODO Base?
            DocumentModel custProductLastVersion = documentManager.getLastDocumentVersion(
                    custProduct.getRef());
            DocumentModel itemLastVersion = documentManager.getLastDocumentVersion(
                    item.getRef());
            if (itemLastVersion != null) {
                eloraDocumentRelationManager.softDeleteRelation(documentManager,
                        custProductLastVersion,
                        EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT,
                        itemLastVersion);
            }
        }
    }

}
