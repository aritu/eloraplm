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

import com.aritu.eloraplm.bom.treetable.ManufacturerPartInverseTreeBean;
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

@Name("manufacturerRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class EloraManufacturerRelationBean
        extends EloraDocContextBoundActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private EloraVersionLabelService eloraVersionLabelService = Framework.getService(
            EloraVersionLabelService.class);

    private List<Statement> outgoingManufacturerPartStatements;

    private List<StatementInfo> outgoingManufacturerPartStatementsInfo;

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
    private ManufacturerPartInverseTreeBean manufacturerPartInverseTreeBean;

    // Add Relation form properties

    private String predicateUri;

    private String manufacturer;

    private String reference;

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public EloraManufacturerRelationBean() {
    }

    public void initData() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        reference = (String) currentDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        predicateUri = EloraRelationConstants.BOM_MANUFACTURER_HAS_PART;
    }

    @Factory(value = "outgoingManufacturerPartRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingBomManufacturerStatementsInfo() {
        if (outgoingManufacturerPartStatementsInfo != null) {
            return outgoingManufacturerPartStatementsInfo;
        }

        DocumentModel currentDoc = documentManager.getLastDocumentVersion(
                getCurrentDocument().getRef());

        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_MANUFACTURER_HAS_PART);
        outgoingManufacturerPartStatements = RelationHelper.getStatements(
                currentDoc, predicate);

        if (outgoingManufacturerPartStatements.isEmpty()) {
            outgoingManufacturerPartStatements = Collections.emptyList();
            outgoingManufacturerPartStatementsInfo = Collections.emptyList();
        } else {
            outgoingManufacturerPartStatementsInfo = eloraRelationActions.getStatementsInfo(
                    outgoingManufacturerPartStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingManufacturerPartStatementsInfo, comp);
        }
        return outgoingManufacturerPartStatementsInfo;
    }

    public String addRelation() throws EloraException {
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        try {

            DocumentModel manPartDoc = createManufacturerPart(currentDoc);

            // Check in new document
            EloraDocumentHelper.setupCheckIn(eloraVersionLabelService,
                    manPartDoc, "Created from Part: " + currentDoc.getTitle());

            manPartDoc = documentManager.saveDocument(manPartDoc);

            resetCreateFormValues();

            addRelations(currentDoc, manPartDoc);

        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }

        resetManufacturerPartStatements();

        manufacturerPartInverseTreeBean.createRoot();

        return null;
    }

    private DocumentModel createManufacturerPart(DocumentModel doc)
            throws EloraException {
        String structureRootId = EloraStructureHelper.getStructureRootUid(doc,
                documentManager);

        String targetFolderPath = EloraStructureHelper.getPathByType(
                new IdRef(structureRootId),
                EloraDoctypeConstants.BOM_MANUFACTURER_PART,
                EloraDoctypeConstants.STRUCTURE_EBOM, documentManager);

        String manName = EloraConfigHelper.getManufacturerConfig(manufacturer);
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String pathManName = pss.generatePathSegment(manName);
        DocumentModel manPartDoc = documentManager.createDocumentModel(
                targetFolderPath, pathManName,
                EloraDoctypeConstants.BOM_MANUFACTURER_PART);

        manPartDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                manName);
        manPartDoc.setPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE,
                reference);
        manPartDoc.setPropertyValue(
                EloraMetadataConstants.ELORA_BOMMANPART_MANUFACTURER,
                manufacturer);

        return documentManager.createDocument(manPartDoc);
    }

    private void addRelations(DocumentModel currentDoc,
            DocumentModel manPartDoc) {
        // Add inverse relation taking manPartDoc as subject
        eloraDocumentRelationManager.addRelation(documentManager, manPartDoc,
                currentDoc, predicateUri, "", "1");

        DocumentModel manPartDocLastVersion = documentManager.getLastDocumentVersion(
                manPartDoc.getRef());
        DocumentModel currentDocLastVersion = documentManager.getLastDocumentVersion(
                currentDoc.getRef());
        // Add inverse relation taking custProdDocLastVersion as subject, av->av
        eloraDocumentRelationManager.addRelation(documentManager,
                manPartDocLastVersion, currentDocLastVersion, predicateUri, "",
                "1");

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("label.relation.created"));
    }

    private void resetManufacturerPartStatements() {
        outgoingManufacturerPartStatements = null;
        outgoingManufacturerPartStatementsInfo = null;
    }

    private void resetCreateFormValues() {
        manufacturer = null;
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
        resetManufacturerPartStatements();
        initData();
    }
}