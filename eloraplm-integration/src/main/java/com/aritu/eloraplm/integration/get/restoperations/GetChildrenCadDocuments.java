/**
 *
 */

package com.aritu.eloraplm.integration.get.restoperations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetChildrenCadDocumentsResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetChildrenCadDocumentsResponseDoc;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.ItemInfo;

/**
 * @author
 */
@Operation(id = GetChildrenCadDocuments.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get children cad documents", description = "")
public class GetChildrenCadDocuments {

    public static final String ID = "Elora.PlmConnector.GetChildrenCadDocuments";

    private static final Log log = LogFactory.getLog(
            GetChildrenCadDocuments.class);

    @Context
    protected CoreSession session;

    /** Filter that hides HiddenInNavigation and deleted objects. */
    protected Filter documentFilter;

    protected GetChildrenCadDocumentsResponse getChildrenCadDocumentsResponse;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "parentRealUid", required = true)
    public DocumentRef parentRealRef;

    @Param(name = "getItemsInfo", required = true)
    protected boolean getItemsInfo;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        getChildrenCadDocumentsResponse = new GetChildrenCadDocumentsResponse();

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            DocumentModel targetDoc = session.getDocument(parentRealRef);

            if (targetDoc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                DocumentModelList itemCadDocuments = getItems(targetDoc);

                for (DocumentModel itemCadDocument : itemCadDocuments) {
                    getChildrenCadDocumentsResponse.addDocument(
                            processDocumentChild(itemCadDocument));
                }

            } else {
                getCadDocumentChildren();
                getFolderChildren();
                getItemChildren();
            }

            getChildrenCadDocumentsResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getChildrenCadDocumentsResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getChildrenCadDocumentsResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getChildrenCadDocumentsResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getChildrenCadDocumentsResponse.setErrorMessage(e.getMessage());
            getChildrenCadDocumentsResponse.emptyDocuments();
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getChildrenCadDocumentsResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getChildrenCadDocumentsResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            getChildrenCadDocumentsResponse.emptyDocuments();
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return getChildrenCadDocumentsResponse.convertToJson();
    }

    protected DocumentModelList getItems(DocumentModel targetDoc)
            throws EloraException {
        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModel itemDocument = targetDoc.isProxy()
                ? session.getSourceDocument(targetDoc.getRef()) : targetDoc;

        return RelationHelper.getObjectDocuments(itemDocument, predicate);

    }

    protected void getCadDocumentChildren() throws EloraException {
        String logInitMsg = "[getCadDocumentChildren] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModelList children = session.getChildren(parentRealRef, null,
                getCadDocumentFilter(), null);

        for (DocumentModel child : children) {

            getChildrenCadDocumentsResponse.addDocument(
                    processDocumentChild(child));
        }
        log.trace(logInitMsg + children.size() + "| children found.");
    }

    protected void getFolderChildren() throws EloraException {
        String logInitMsg = "[getFolderChildren] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModelList children = session.getChildren(parentRealRef, null,
                getFolderDocumentFilter(), null);

        for (DocumentModel child : children) {

            getChildrenCadDocumentsResponse.addDocument(
                    processFolderChild(child));
        }
        log.trace(logInitMsg + children.size() + "| children found.");
    }

    protected void getItemChildren() throws EloraException {
        String logInitMsg = "[getItemChildren] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModelList children = session.getChildren(parentRealRef, null,
                getBomDocumentFilter(), null);

        for (DocumentModel child : children) {
            getChildrenCadDocumentsResponse.addDocument(
                    processDocumentChild(child));
        }

        log.trace(logInitMsg + children.size() + "| children found.");
    }

    // TODO: Meter esto en una clase generica
    /** Gets the filter that hides HiddenInNavigation and deleted objects. */
    protected Filter getCadDocumentFilter() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION, false));
        filters.add(
                new FacetFilter(EloraFacetConstants.FACET_CAD_DOCUMENT, true));
        filters.add(
                new LifeCycleFilter(LifeCycleConstants.DELETED_STATE, false));

        return new CompoundFilter(filters);
    }

    // TODO: Meter esto en una clase generica
    /** Gets the filter that hides HiddenInNavigation and deleted objects. */
    protected Filter getBomDocumentFilter() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION, false));
        filters.add(
                new FacetFilter(EloraFacetConstants.FACET_BOM_DOCUMENT, true));
        filters.add(
                new LifeCycleFilter(LifeCycleConstants.DELETED_STATE, false));

        return new CompoundFilter(filters);
    }

    // TODO: Meter esto en una clase generica
    protected Filter getFolderDocumentFilter() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION, false));
        filters.add(new FacetFilter(FacetNames.FOLDERISH, true));
        filters.add(
                new LifeCycleFilter(LifeCycleConstants.DELETED_STATE, false));

        return new CompoundFilter(filters);
    }

    protected GetChildrenCadDocumentsResponseDoc processDocumentChild(
            DocumentModel doc) throws EloraException {

        GetChildrenCadDocumentsResponseDoc responseDoc = null;
        List<ItemInfo> itemsInfo = null;

        responseDoc = new GetChildrenCadDocumentsResponseDoc();

        String realUid = null;
        String proxyUid = null;
        DocumentModel wcDoc = null;

        if (doc.isProxy()) {
            proxyUid = doc.getId();
            doc = session.getSourceDocument(doc.getRef());
        }

        if (doc.isImmutable()) {
            realUid = doc.getId();
            wcDoc = session.getWorkingCopy(doc.getRef());
        } else {
            DocumentModel lastVersionDoc = EloraDocumentHelper.getLatestVersion(
                    doc);
            if (lastVersionDoc == null) {
                // If document has no version returns null. This is
                // because it is not possible to checkout or get
                // this document so it is not shown
                return null;
            }
            realUid = lastVersionDoc.getId();
            wcDoc = doc;
        }

        String curLifeCycleState = doc.getCurrentLifeCycleState();
        EloraLockInfo lockInfo = EloraDocumentHelper.getLockInfo(wcDoc);

        String title = doc.getTitle();
        String reference = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        VersionInfo currentVersionInfo = EloraIntegrationHelper.createVersionInfo(
                doc, wcDoc);

        responseDoc.setParentRealUid(parentRealRef.toString());
        responseDoc.setRealUid(realUid);
        responseDoc.setWcUid(wcDoc.getId());
        responseDoc.setReference(reference);
        responseDoc.setType(doc.getType());
        responseDoc.setTitle(title);
        responseDoc.setPath(FilenameUtils.getPath(doc.getPathAsString()));
        responseDoc.setCurrentLifeCycleState(curLifeCycleState);
        responseDoc.setEloraLockInfo(lockInfo);
        responseDoc.setProxyUid(proxyUid);
        responseDoc.setCurrentVersionInfo(currentVersionInfo);

        if (getItemsInfo) {
            itemsInfo = EloraIntegrationHelper.getItemsInfo(session, doc, true);
            responseDoc.setItemsInfo(itemsInfo);
        }

        if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            String authoringTool = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL) == null
                            ? ""
                            : doc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL).toString();

            String authoringToolVersion = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION) == null
                            ? ""
                            : doc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION).toString();

            responseDoc.setAuthoringTool(authoringTool);
            responseDoc.setAuthoringToolVersion(authoringToolVersion);
            responseDoc.setHasChildren(false);
        } else {
            responseDoc.setHasChildren(true);
        }
        return responseDoc;
    }

    protected GetChildrenCadDocumentsResponseDoc processFolderChild(
            DocumentModel doc) throws EloraException {

        GetChildrenCadDocumentsResponseDoc responseDoc = new GetChildrenCadDocumentsResponseDoc();

        responseDoc.setParentRealUid(parentRealRef.toString());
        responseDoc.setRealUid(doc.getId());
        responseDoc.setType(doc.getType());
        responseDoc.setTitle(doc.getTitle());
        responseDoc.setPath(FilenameUtils.getPath(doc.getPathAsString()));
        responseDoc.setHasChildren(true);

        return responseDoc;
    }

}
