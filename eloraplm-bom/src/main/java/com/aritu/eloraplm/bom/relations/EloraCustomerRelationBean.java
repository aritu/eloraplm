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
package com.aritu.eloraplm.bom.relations;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.bom.treetable.CustomerProductInverseTreeBean;
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.EloraRelationActionsBean;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("customerRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class EloraCustomerRelationBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private EloraVersionLabelService eloraVersionLabelService = Framework.getService(
            EloraVersionLabelService.class);

    private List<Statement> outgoingCustomerProductStatements;

    private List<StatementInfo> outgoingCustomerProductStatementsInfo;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true, required = false)
    private transient EloraRelationActionsBean eloraRelationActions;

    @In
    private transient NavigationContext navigationContext;

    @In(create = true, required = false)
    private transient FacesMessages facesMessages;

    @In(create = true)
    private Map<String, String> messages;

    @In(create = true)
    private EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    private transient WebActions webActions;

    @In(create = true)
    private CustomerProductInverseTreeBean customerProductInverseTreeBean;

    // Add Relation form properties

    private String predicateUri;

    private String customer;

    private String reference;

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public EloraCustomerRelationBean() {
    }

    public void initData() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        reference = (String) currentDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        predicateUri = EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT;
    }

    @Factory(value = "outgoingCustomerProductRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingBomCustomerStatementsInfo() {
        if (outgoingCustomerProductStatementsInfo != null) {
            return outgoingCustomerProductStatementsInfo;
        }

        // Get last version to show its relations. We don't check if it is
        // checked out because is incoming and we show just AV -> AV relations
        DocumentModel currentDoc = documentManager.getLastDocumentVersion(
                getCurrentDocument().getRef());

        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT);
        outgoingCustomerProductStatements = RelationHelper.getStatements(
                currentDoc, predicate);

        if (outgoingCustomerProductStatements.isEmpty()) {
            outgoingCustomerProductStatements = Collections.emptyList();
            outgoingCustomerProductStatementsInfo = Collections.emptyList();
        } else {
            outgoingCustomerProductStatementsInfo = eloraRelationActions.getStatementsInfo(
                    outgoingCustomerProductStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingCustomerProductStatementsInfo, comp);
        }
        return outgoingCustomerProductStatementsInfo;
    }

    public String addRelation() throws EloraException {
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        try {
            DocumentModel custProdDoc = createCustomerProduct(currentDoc);

            // Check in new document
            EloraDocumentHelper.setupCheckIn(eloraVersionLabelService,
                    custProdDoc,
                    "Created from Product: " + currentDoc.getTitle());

            custProdDoc = documentManager.saveDocument(custProdDoc);

            resetCreateFormValues();

            addRelations(currentDoc, custProdDoc);

        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }

        resetCustomerProductStatements();

        customerProductInverseTreeBean.createRoot();

        return null;
    }

    private DocumentModel createCustomerProduct(DocumentModel doc)
            throws EloraException {
        String structureRootId = EloraStructureHelper.getStructureRootUid(doc,
                documentManager);

        String targetFolderPath = EloraStructureHelper.getPathByType(
                new IdRef(structureRootId),
                EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT,
                EloraDoctypeConstants.STRUCTURE_EBOM, documentManager);

        String custName = EloraConfigHelper.getCustomerConfig(customer);
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String pathCustName = pss.generatePathSegment(custName);
        DocumentModel custProdDoc = documentManager.createDocumentModel(
                targetFolderPath, pathCustName,
                EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT);

        custProdDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                custName);
        custProdDoc.setPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE,
                reference);
        custProdDoc.setPropertyValue(
                EloraMetadataConstants.ELORA_BOMCUSTPROD_CUSTOMER, customer);

        return documentManager.createDocument(custProdDoc);
    }

    private void addRelations(DocumentModel currentDoc,
            DocumentModel custProdDoc) {
        // Add inverse relation taking custProdDoc as subject, wc -> wc
        eloraDocumentRelationManager.addRelation(documentManager, custProdDoc,
                currentDoc, predicateUri, "", "1");

        // We don't use eloraDocumentHelper.getLatestVersion because in this
        // case this returns the same and maybe is more efficient
        DocumentModel custProdDocLastVersion = documentManager.getLastDocumentVersion(
                custProdDoc.getRef());
        DocumentModel currentDocLastVersion = documentManager.getLastDocumentVersion(
                currentDoc.getRef());
        // Add inverse relation taking custProdDocLastVersion as subject, av->av
        eloraDocumentRelationManager.addRelation(documentManager,
                custProdDocLastVersion, currentDocLastVersion, predicateUri, "",
                "1");

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("label.relation.created"));
    }

    private void resetCustomerProductStatements() {
        outgoingCustomerProductStatements = null;
        outgoingCustomerProductStatementsInfo = null;
    }

    private void resetCreateFormValues() {
        customer = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.aritu.eloraplm.core.EloraDocContextBoundActionBean#resetBeanCache
     * (org.nuxeo.ecm.core.api.DocumentModel)
     */
    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCustomerProductStatements();
        initData();
    }
}