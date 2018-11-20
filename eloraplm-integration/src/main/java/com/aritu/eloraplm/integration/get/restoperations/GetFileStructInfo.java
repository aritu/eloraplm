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

package com.aritu.eloraplm.integration.get.restoperations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.config.util.PropagationConfig;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileStructInfoResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileStructInfoResponseDoc;
import com.aritu.eloraplm.integration.get.restoperations.util.PropagationProperty;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.ItemInfo;
import com.aritu.eloraplm.integration.util.RelationInfo;
import com.aritu.eloraplm.integration.util.VersionItem;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

/**
 * @author aritu
 *
 */
@Operation(id = GetFileStructInfo.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get File Structure Info", description = "Get information of the document and its child relations.")
public class GetFileStructInfo {
    public static final String ID = "Elora.PlmConnector.GetFileStructInfo";

    private static final Log log = LogFactory.getLog(GetFileStructInfo.class);

    private static final String ACTION_CHECKOUT = "Checkout";

    private static final String ACTION_GET = "Get";

    private static final String[] ACTIONS = { ACTION_CHECKOUT, ACTION_GET };

    private static final String CHILDREN_VERSIONS_AS_STORED = "AsStored";

    private static final String CHILDREN_VERSIONS_LATEST_VERSIONS = "LatestVersions";

    private static final String CHILDREN_VERSIONS_LATEST_RELEASED = "LatestReleased";

    private static final String[] CHILDREN_VERSIONS = {
            CHILDREN_VERSIONS_AS_STORED, CHILDREN_VERSIONS_LATEST_VERSIONS,
            CHILDREN_VERSIONS_LATEST_RELEASED };

    private static final String RELATION_SUBTYPE_HIERARCHICAL = "Hierarchical";

    private static final String RELATION_SUBTYPE_DIRECT_AND_ICONONLY = "DirectAndIconOnly";

    @Context
    private OperationContext ctx;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "action", required = true)
    private String action;

    @Param(name = "proxyUid", required = false)
    private DocumentRef proxyRef;

    @Param(name = "realUid", required = true)
    private DocumentRef realRef;

    @Param(name = "isRootElement", required = false)
    private boolean isRootElement;

    @Param(name = "rootRealUid", required = false)
    private DocumentRef rootRealRef;

    @Param(name = "newVersionRealUid", required = false)
    private DocumentRef newVersionRealRef;

    @Param(name = "childrenVersions", required = true)
    private String childrenVersions;

    @Param(name = "getItemsInfo", required = true)
    protected boolean getItemsInfo;

    @Param(name = "enforce", required = true)
    private boolean isTargetEnforce;

    @Param(name = "selected", required = true)
    private boolean isTargetSelected;

    @Context
    private CoreSession session;

    private RelationManager relationManager = Framework.getService(
            RelationManager.class);

    private EloraVersionLabelService versionLabelService = Framework.getService(
            EloraVersionLabelService.class);

    private EloraConfigTable propagationConfig;

    private List<String> cadParents;

    private GetFileStructInfoResponse getFileStructInfoResponse;

    private boolean isInitialLoad;

    private boolean isTargetWc;

    private boolean isRootSpecial;

    private String rootSpecialWcUid;

    private DocumentModel targetDoc;

    private List<Resource> hierarchicalAndSuppressedPredicates;

    private List<Resource> directAndIconOnlyPredicates;

    private List<Resource> specialPredicates;

    private class SpecialDocRepeatedException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public SpecialDocRepeatedException(String message) {
            super(message);
        }
    }

    private class DocIsInCadParentsException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public DocIsInCadParentsException(String message) {
            super(message);
        }
    }

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "action: |" + action + "|" + "\n" + "proxyUid: |"
                + proxyRef + "|" + "\n" + "realUid: |" + realRef + "|" + "\n"
                + "isRootElement: |" + isRootElement + "|" + "\n"
                + "rootRealUid: |" + rootRealRef + "|" + "\n"
                + "newVersionRealUid: |" + newVersionRealRef + "|" + "\n"
                + "childrenVersions: |" + childrenVersions + "|");

        getFileStructInfoResponse = new GetFileStructInfoResponse();

        hierarchicalAndSuppressedPredicates = new ArrayList<Resource>();
        directAndIconOnlyPredicates = new ArrayList<Resource>();
        specialPredicates = new ArrayList<Resource>();

        cadParents = new ArrayList<>();

        isInitialLoad = true;
        isTargetWc = false;
        isRootSpecial = false;

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            checkInputData();

            loadConfigAndData();

            processDocumentAndRelations(1, true, targetDoc, null, null,
                    isTargetSelected, checkIfWeGetFirstLevelRelations());

            log.info("Document and children successfuly processed. "
                    + getFileStructInfoResponse.getDocuments().size()
                    + " documents in tree.");
            getFileStructInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getFileStructInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getFileStructInfoResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getFileStructInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getFileStructInfoResponse.setErrorMessage(e.getMessage());
            getFileStructInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getFileStructInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getFileStructInfoResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            getFileStructInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        String jsonResponse = getFileStructInfoResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    private void checkInputData() throws EloraException {
        checkIfActionIsAllowed();
        checkIfRealRefExists();
        isInitialLoad = newVersionRealRef == null ? true : false;
        checkIfChildrenVersionsOptionIsAllowed();
        checkIfRootIsSpecial();
    }

    private void checkIfActionIsAllowed() throws EloraException {
        if (!Arrays.asList(ACTIONS).contains(action)) {
            throw new EloraException("The action must be "
                    + String.join(" or ", Arrays.asList(ACTIONS)) + ".");
        }
    }

    private void checkIfRealRefExists() throws EloraException {
        if (realRef == null) {
            throw new EloraException(
                    "Real uid of the target document is null.");
        }
    }

    private void checkIfChildrenVersionsOptionIsAllowed()
            throws EloraException {
        if (!isInitialLoad
                && !childrenVersions.equals(CHILDREN_VERSIONS_AS_STORED)) {
            throw new EloraException(
                    "Children versions option must be AsStored if new version is selected.");
        }
        if (isInitialLoad && !Arrays.asList(CHILDREN_VERSIONS).contains(
                childrenVersions)) {

            throw new EloraException("The children versions option must be "
                    + String.join(" or ", Arrays.asList(CHILDREN_VERSIONS))
                    + ".");
        }
    }

    private void checkIfRootIsSpecial() {
        DocumentModel rootDoc = isRootElement ? session.getDocument(realRef)
                : session.getDocument(rootRealRef);
        if (rootDoc != null && rootDoc.getType().equals(
                EloraDoctypeConstants.CAD_DRAWING)) {
            isRootSpecial = true;
        }
    }

    private void loadConfigAndData() throws EloraException {

        loadConfigurations();

        getTargetDoc();

        getRootDocInfoIfSpecial();
    }

    /**
     * Loads the required configurations for the operation
     *
     * @throws EloraException
     */
    private void loadConfigurations() throws EloraException {

        String logInitMsg = "[loadConfigurations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (action.equals(ACTION_CHECKOUT)) {
            propagationConfig = PropagationConfig.checkoutPropagationConfig;
        } else {
            propagationConfig = PropagationConfig.getPropagationConfig;
        }

        log.trace(logInitMsg + "Relation propagation configuration loaded.");

        loadSpecialRelations();
        loadHierarchicalAndSuppressedRelations();
        loadDirectAndIconOnlyRelations();

        log.trace(logInitMsg + "Relations configuration loaded.");

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void loadSpecialRelations() {
        for (String predicateUri : RelationsConfig.cadSpecialRelationsList) {
            specialPredicates.add(new ResourceImpl(predicateUri));
        }
    }

    private void loadHierarchicalAndSuppressedRelations() {
        List<String> hierarchicalAndSuppressedRelationsList = new ArrayList<String>();
        hierarchicalAndSuppressedRelationsList.addAll(
                RelationsConfig.cadHierarchicalRelationsList);
        hierarchicalAndSuppressedRelationsList.addAll(
                RelationsConfig.cadSuppressedRelationsList);
        for (String predicateUri : hierarchicalAndSuppressedRelationsList) {
            hierarchicalAndSuppressedPredicates.add(
                    new ResourceImpl(predicateUri));
        }
    }

    private void loadDirectAndIconOnlyRelations() {
        List<String> directAndIconOnlyRelationsList = new ArrayList<String>();
        directAndIconOnlyRelationsList.addAll(
                RelationsConfig.cadDirectRelationsList);
        directAndIconOnlyRelationsList.addAll(
                RelationsConfig.cadIconOnlyRelationsList);
        for (String predicateUri : directAndIconOnlyRelationsList) {
            directAndIconOnlyPredicates.add(new ResourceImpl(predicateUri));
        }
    }

    /**
     * Gets the target, which is the real document that we are getting info
     * about, It is the realRef in the initial load, but if a version change is
     * requested, the new version document becomes the target
     *
     * @return
     * @throws EloraException
     */
    private DocumentModel getTargetDoc() throws EloraException {

        String logInitMsg = "[getTargetDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (isInitialLoad) {
            targetDoc = session.getDocument(realRef);
        } else {
            targetDoc = session.getDocument(newVersionRealRef);

            log.info("Version change requested to uid |" + targetDoc.getId()
                    + "|.");
        }

        isTargetWc = targetDoc.isVersion() ? false : true;

        checkIfTargetDocIsCorrect();

        log.trace(logInitMsg + "--- EXIT ---");

        return targetDoc;
    }

    /**
     * @throws EloraException
     */
    private void checkIfTargetDocIsCorrect() throws EloraException {

        if (targetDoc.isProxy()) {
            throw new EloraException("The target document is a proxy");
        }

        if (action.equals(ACTION_CHECKOUT)) {
            // Checkout action does not allow WC docs
            if (isTargetWc) {
                throw new EloraException(
                        "The target document is a working copy and action is |"
                                + ACTION_CHECKOUT + "|.");
            }

            // Checkout action does not allow obsoletes
            if (EloraDocumentHelper.isAvObsolete(targetDoc)) {
                throw new EloraException(
                        "The target document can not be obsolete with action Checkout.");
            }
        }

    }

    /**
     * If it is not the initial load, we need to check if the root document is
     * special
     */
    private void getRootDocInfoIfSpecial() {

        if (!isInitialLoad) {
            if (isRootSpecial) {
                DocumentModel rootSpecialWc = session.getWorkingCopy(
                        rootRealRef);
                rootSpecialWcUid = rootSpecialWc.getId();
            }
        }
    }

    /**
     * When the version of a special document is changed, and it is not the root
     * element, do not return children.
     *
     * @return
     */
    private boolean checkIfWeGetFirstLevelRelations() {
        if (!isInitialLoad && !isRootElement && targetDoc.getType().equals(
                EloraDoctypeConstants.CAD_DRAWING)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Processes the document, obtaining the necessary info, and also its
     * relations if needed
     *
     * @param level
     * @param doc
     * @param cadParentStatement
     * @param cadParentRealUid
     * @param isCadParentSelected
     * @param getRelations
     * @throws EloraException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    protected void processDocumentAndRelations(int level, boolean isTargetDoc,
            DocumentModel doc, Statement cadParentStatement,
            String cadParentRealUid, boolean isCadParentSelected,
            boolean getRelations) throws EloraException, IllegalAccessException,
            InvocationTargetException {

        String logInitMsg = "[processDocumentAndRelations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "CAD Parents: |" + cadParents.toString() + "|");
        String docId = doc.getId();

        PropagationProperty propagationProp = getPropagationProperty(
                isTargetDoc, isCadParentSelected, new PropagationProperty(),
                cadParentStatement);

        if (!isTargetDoc) {
            // If the relation is special, get unique document for different
            // versions
            // Else, switch versions if necessary
            doc = getCorrectVersionDocument(level, doc, cadParentStatement,
                    isCadParentSelected);
            docId = doc.getId();
        }

        try {
            checkIsNotInCadParentsAndAddIt(docId);

            updateOrAddDocument(level, isTargetDoc, doc, cadParentStatement,
                    cadParentRealUid, isCadParentSelected, getRelations, docId,
                    propagationProp);

            cadParents.remove(cadParents.size() - 1);

        } catch (DocIsInCadParentsException e) {
            // TODO Beharrezkoa??
            log.trace(logInitMsg + "Document |" + docId
                    + "| is a CAD parent. LOG TRACE");
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    /**
     * Check if the document is stored in the map, to only update the relations
     * and checkout propagation property (if needed), or to add it as new
     *
     * @param level
     * @param isTargetDoc
     * @param doc
     * @param cadParentStatement
     * @param cadParentRealUid
     * @param isCadParentMarkedToCheckOut
     * @param getRelations
     * @param docId
     * @param checkoutProp
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws EloraException
     */
    private void updateOrAddDocument(int level, boolean isTargetDoc,
            DocumentModel doc, Statement cadParentStatement,
            String cadParentRealUid, boolean isCadParentSelected,
            boolean getRelations, String docId,
            PropagationProperty propagationProp) throws IllegalAccessException,
            InvocationTargetException, EloraException {
        // Check if the document is stored in the map, to only update the
        // relations and checkout propagation property (if needed)
        if (getFileStructInfoResponse.hasDocument(docId)) {
            updateRelationsAndPropagationProperty(isTargetDoc, docId,
                    cadParentStatement, cadParentRealUid, isCadParentSelected);
        } else {
            addDocumentAndProcessRelations(level, isTargetDoc, doc,
                    cadParentStatement, cadParentRealUid, isCadParentSelected,
                    propagationProp, getRelations);
        }
    }

    private void checkIsNotInCadParentsAndAddIt(String docId)
            throws DocIsInCadParentsException {
        // If document exists in cad parents, cut the process
        if (cadParents.contains(docId)) {
            throw new DocIsInCadParentsException(
                    "Document |" + docId + "| is a CAD parent.");
        } else {
            cadParents.add(docId);
        }

    }

    private DocumentModel getCorrectVersionDocument(int level,
            DocumentModel doc, Statement cadParentStatement,
            boolean isCadParentSelected) throws EloraException {

        String logInitMsg = "[getCorrectVersionDocument] ["
                + session.getPrincipal().getName() + "] ";

        boolean isSpecial = getIsSpecialRelation(cadParentStatement);
        if (isSpecial) {

            // It is a direct relation, so we don't switch. We get the
            // latest related released version.

            // TODO Hau aldatu????
            DocumentModel latestRelatedReleasedDoc = EloraRelationHelper.getLatestRelatedReleasedVersion(
                    doc, cadParentStatement, session);

            if (latestRelatedReleasedDoc != null) {
                doc = latestRelatedReleasedDoc;
            }

        } else {

            // Switch versions only in the initial load
            if (isInitialLoad) {

                if (hasToSwitchVersions(level, isCadParentSelected)) {
                    String previousDocId = doc.getId();
                    doc = switchVersion(doc);
                    String newDocId = doc.getId();

                    if (!previousDocId.equals(newDocId)) {

                        log.trace(logInitMsg + "Document version switched to |"
                                + newDocId + "|.");
                    }
                }

            }
        }
        return doc;

    }

    /**
     * When to switch, considering it is initial load:
     *
     * - Root element is special (drawing), and level is 3 (we switch drawing's
     * grandchildren)
     *
     * - Root element is normal, and level is 2 (we switch normal documents'
     * children)
     *
     * - Parent node is selected
     *
     * @param level
     * @param isCadParentSelected
     * @return
     */
    private boolean hasToSwitchVersions(int level,
            boolean isCadParentSelected) {

        //
        //
        // TODO Hau Josukin begiratu: GETen be berdina da? CHECKOUTen hola
        // erabaki zan.
        //
        //

        if ((isRootSpecial && level == 3) || (!isRootSpecial && level == 2)
                || isCadParentSelected) {
            return true;
        } else {
            return false;
        }
    }

    private void updateRelationsAndPropagationProperty(boolean isTargetDoc,
            String docId, Statement cadParentStatement, String cadParentRealUid,
            boolean isCadParentSelected) throws IllegalAccessException,
            InvocationTargetException, EloraException {

        String logInitMsg = "[updateRelationsAndPropagationProperty] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Document |" + docId
                + "| is already in response map. Updating relations and propagation (if needed)...");

        Map<String, GetFileStructInfoResponseDoc> responseDocEntries = getFileStructInfoResponse.getDocumentEntries(
                docId);

        // Create the relation info
        RelationInfo relationInfo = createRelationInfo(cadParentStatement);

        // Check if same parent exists
        GetFileStructInfoResponseDoc auxResponseDoc;
        if (responseDocEntries.containsKey(cadParentRealUid)) {
            auxResponseDoc = addNewRelationInfoToDocEntry(cadParentRealUid,
                    responseDocEntries, relationInfo);
        } else {
            auxResponseDoc = createNewDocEntry(cadParentRealUid,
                    responseDocEntries, relationInfo);
        }

        updatePropagationForDocEntry(isTargetDoc, cadParentStatement,
                isCadParentSelected, responseDocEntries, auxResponseDoc);

        getFileStructInfoResponse.addDocumentEntries(docId, responseDocEntries);

        log.trace(logInitMsg + "Document |" + docId + "| processed.");

    }

    private void updatePropagationForDocEntry(boolean isTargetDoc,
            Statement cadParentStatement, boolean isCadParentSelected,
            Map<String, GetFileStructInfoResponseDoc> responseDocEntries,
            GetFileStructInfoResponseDoc auxResponseDoc) throws EloraException {

        PropagationProperty currentPropagation = auxResponseDoc.getPropagationProperty();
        PropagationProperty updatedPropagation = getPropagationProperty(
                isTargetDoc, isCadParentSelected, currentPropagation,
                cadParentStatement);

        // Update propagation property values for the document in the
        // hashmap
        for (Map.Entry<String, GetFileStructInfoResponseDoc> entry : responseDocEntries.entrySet()) {
            String key = entry.getKey();
            GetFileStructInfoResponseDoc value = entry.getValue();
            value.setPropagationProperty(updatedPropagation);
            responseDocEntries.put(key, value);
        }
    }

    private GetFileStructInfoResponseDoc createNewDocEntry(
            String cadParentRealUid,
            Map<String, GetFileStructInfoResponseDoc> responseDocEntries,
            RelationInfo relationInfo)
            throws IllegalAccessException, InvocationTargetException {
        GetFileStructInfoResponseDoc auxResponseDoc;
        // If the entry for the CAD parent doesn't exist, create it
        auxResponseDoc = responseDocEntries.values().iterator().next();

        GetFileStructInfoResponseDoc newResponseDoc = new GetFileStructInfoResponseDoc();
        BeanUtils.copyProperties(newResponseDoc, auxResponseDoc);
        newResponseDoc.setCadParentRealUid(cadParentRealUid);
        newResponseDoc.emptyRelations();
        newResponseDoc.addRelation(relationInfo);
        responseDocEntries.put(cadParentRealUid, newResponseDoc);
        return auxResponseDoc;
    }

    private GetFileStructInfoResponseDoc addNewRelationInfoToDocEntry(
            String cadParentRealUid,
            Map<String, GetFileStructInfoResponseDoc> responseDocEntries,
            RelationInfo relationInfo) {
        GetFileStructInfoResponseDoc auxResponseDoc;
        auxResponseDoc = responseDocEntries.get(cadParentRealUid);

        // Update relations
        auxResponseDoc.addRelation(relationInfo);
        responseDocEntries.put(cadParentRealUid, auxResponseDoc);
        return auxResponseDoc;
    }

    /**
     * Process the document for the first time
     *
     * @param level
     * @param isTargetDoc
     * @param doc
     * @param cadParentStatement
     * @param cadParentRealUid
     * @param isCadParentSelected
     * @param propagationProp
     * @param getRelations
     * @throws EloraException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void addDocumentAndProcessRelations(int level, boolean isTargetDoc,
            DocumentModel doc, Statement cadParentStatement,
            String cadParentRealUid, boolean isCadParentSelected,
            PropagationProperty propagationProp, boolean getRelations)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        String logInitMsg = "[addDocumentAndProcessRelations] ["
                + session.getPrincipal().getName() + "] ";

        String docId = doc.getId();
        DocumentRef docProxyRef = (cadParentStatement == null) ? proxyRef
                : null;
        GetFileStructInfoResponseDoc responseDoc = getCommonData(doc.getRef(),
                docProxyRef, doc);

        if (getItemsInfo) {
            List<ItemInfo> itemsInfo = EloraIntegrationHelper.getItemsInfo(
                    session, doc, true);
            responseDoc.setItemsInfo(itemsInfo);
        }

        if (!isTargetDoc) {
            // Create the relation info
            RelationInfo relationInfo = createRelationInfo(cadParentStatement);

            responseDoc.setCadParentRealUid(cadParentRealUid);
            responseDoc.addRelation(relationInfo);
        }
        responseDoc.setPropagationProperty(propagationProp);

        getFileStructInfoResponse.addDocument(docId, cadParentRealUid,
                responseDoc);

        log.trace(logInitMsg + "Document |" + docId + "| processed.");

        // Process relations
        if (getRelations) {
            boolean selected = responseDoc.getSelected();
            processRelations(doc, level, cadParentStatement, selected);

            log.trace(logInitMsg + "Document |" + docId
                    + "| relations processed.");
        }

    }

    private boolean getIsSpecialRelation(Statement cadParentStatement) {

        return RelationsConfig.cadSpecialRelationsList.contains(
                cadParentStatement.getPredicate().getUri());
    }

    /**
     * Switch document's version basing on Switch Children Versions
     * configuration
     *
     * @param doc
     * @return
     * @throws EloraException
     */
    private DocumentModel switchVersion(DocumentModel doc)
            throws EloraException {

        DocumentModel switchedDoc = doc;

        if (!childrenVersions.equals(CHILDREN_VERSIONS_AS_STORED)) {
            if (childrenVersions.equals(CHILDREN_VERSIONS_LATEST_VERSIONS)) {
                switchedDoc = EloraDocumentHelper.getLatestVersion(switchedDoc);
            } else if (childrenVersions.equals(
                    CHILDREN_VERSIONS_LATEST_RELEASED)) {
                switchedDoc = EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                        switchedDoc);
            }
        }
        return switchedDoc;
    }

    /**
     * Creates a RelationInfo object from the statement
     *
     * @param stmt
     * @return
     */
    private RelationInfo createRelationInfo(Statement stmt) {
        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
        RelationInfo relationInfo = new RelationInfo(
                stmtInfo.getPredicate().getUri(), stmtInfo.getQuantity());

        return relationInfo;
    }

    /**
     * Obtains the common data of the document for the response
     *
     * @param docRealRef
     * @param docProxyRef
     * @param doc
     * @return
     * @throws EloraException
     */
    protected GetFileStructInfoResponseDoc getCommonData(DocumentRef docRef,
            DocumentRef docProxyRef, DocumentModel doc) throws EloraException {

        String logInitMsg = "[GetFileStructInfoResponseDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // We know that docRealRef is a version and not a proxy. We get the WC
        DocumentModel wcDoc = session.getSourceDocument(docRef);
        DocumentRef wcDocRef = wcDoc.getRef();

        // TODO Hau queryan bertan egin beharko zan, iterazioetan
        // VersionItemetan sartu, gero berriro for bat erabili behar ez izateko.
        // Dependentzia arazoak...
        List<VersionItem> versions = new ArrayList<>();
        // We don't exclude obsoletes from the list for now, we want them to
        // show in the integrator interface, because the current version can be
        // obsolete and it must be in the list
        //
        // boolean includeObsoletes = !action.equals(ACTION_CHECKOUT);
        boolean includeObsoletes = true;
        Map<String, String> allVersionsInfo = EloraQueryFactory.getAllVersionsInfo(
                session, wcDoc.getId(), includeObsoletes);
        for (Map.Entry<String, String> versionInfo : allVersionsInfo.entrySet()) {
            versions.add(new VersionItem(versionInfo.getKey(),
                    versionLabelService.translateVersionLabel(
                            versionInfo.getValue())));
        }

        String latestVersionLabel = wcDoc.getVersionLabel();

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(wcDoc);

        // Get the view URL
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String summaryUrl = EloraUrlHelper.getDocumentSummaryInPrintModeUrl(
                request, doc);

        Blob contentBlob = (Blob) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        String filename = null;
        String hash = null;
        if (contentBlob != null) {
            filename = contentBlob.getFilename();
            hash = contentBlob.getDigest();
        }

        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            lastModified = lastModifiedGc.getTime();
        }

        String reference = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String title = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String lastContributor = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR).toString();

        String proxyUid = null;

        if (docProxyRef != null) {
            proxyUid = docProxyRef.toString();
        }

        /*
        else {
            // Check if doc exists in workspace, and get the proxyUid
            List<String> docIds = new ArrayList<String>();
        
            docIds.add(docRef.toString());
            String query = EloraQueryFactory.getDocProxiesInWorkspaceQuery(
                    docIds, workspaceRealUid);
            DocumentModelList docProxies = session.query(query);
        
            // If the exact version doesn't exist, search for live proxies
            // (WC)
            if (docProxies == null || docProxies.isEmpty()) {
                docIds.add(wcDocRef.toString());
                query = EloraQueryFactory.getDocProxiesInWorkspaceQuery(docIds,
                        workspaceRealUid);
                docProxies = session.query(query);
            }
        
            if (docProxies != null && !docProxies.isEmpty()) {
                DocumentModel proxyDoc = docProxies.get(0);
                proxyUid = proxyDoc.getId();
            }
        }
        */

        // If target document is a working copy, complete some data with latest
        // version
        String realUid = docRef.toString();

        VersionInfo currentVersionInfo = EloraIntegrationHelper.createVersionInfo(
                doc, wcDoc);

        GetFileStructInfoResponseDoc responseDoc = new GetFileStructInfoResponseDoc(
                proxyUid, realUid, wcDoc.getId(), doc.getType(), reference,
                title, filename, hash, description, currentVersionInfo,
                doc.getCurrentLifeCycleState(), latestVersionLabel, versions,
                eloraLockInfo, lastContributor, lastModified, summaryUrl);

        log.trace(logInitMsg + "--- EXIT ---");

        return responseDoc;
    }

    /**
     * Processes the hierarchical and direct relations of the document
     *
     * @param doc
     * @param level
     * @param cadParentStatement
     * @param checkout
     * @throws EloraException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    protected void processRelations(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        String logInitMsg = "[processRelations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        processHierarchicalRelations(doc, level, cadParentStatement, selected);

        processSpecialRelations(doc, level, cadParentStatement, selected);

        processDirectAndIconOnlyRelations(doc, level, cadParentStatement,
                selected);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processHierarchicalRelations(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        List<Statement> statements = getNormalStatements(doc,
                RELATION_SUBTYPE_HIERARCHICAL,
                hierarchicalAndSuppressedPredicates);

        processNormalStatements(doc, level, cadParentStatement, selected,
                RELATION_SUBTYPE_HIERARCHICAL, statements);
    }

    private void processDirectAndIconOnlyRelations(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        List<Statement> statements = getNormalStatements(doc,
                RELATION_SUBTYPE_DIRECT_AND_ICONONLY,
                directAndIconOnlyPredicates);

        processNormalStatements(doc, level, cadParentStatement, selected,
                RELATION_SUBTYPE_DIRECT_AND_ICONONLY, statements);
    }

    private void processSpecialRelations(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        List<Statement> statements = getSpecialStatements(doc, level);

        processSpecialStatements(doc, level, cadParentStatement, selected,
                statements);
    }

    private List<Statement> getNormalStatements(DocumentModel doc,
            String relationSubtype, List<Resource> predicateList) {
        String logInitMsg = "[getNormalStatements] ["
                + session.getPrincipal().getName() + "] ";

        List<Statement> statements = new ArrayList<Statement>();
        if (!predicateList.isEmpty()) {
            statements = EloraRelationHelper.getStatements(doc, predicateList);

            log.trace(logInitMsg + "Document |" + doc.getId() + "| Found "
                    + statements.size() + " " + relationSubtype + " children.");
        }

        return statements;

    }

    private List<Statement> getSpecialStatements(DocumentModel doc, int level) {
        String logInitMsg = "[getSpecialStatements] ["
                + session.getPrincipal().getName() + "] ";

        List<Statement> statements = new ArrayList<Statement>();
        if (!specialPredicates.isEmpty()) {

            if (level == 1 && isRootSpecial && isRootElement) {
                statements = EloraRelationHelper.getStatements(doc,
                        specialPredicates);

                log.trace(logInitMsg + "Document |" + doc.getId() + "| Found "
                        + statements.size()
                        + " special children in the first level.");
            } else {
                statements = EloraRelationHelper.getSubjectStatementsByPredicateList(
                        doc, specialPredicates);

                log.trace(logInitMsg + "Document |" + doc.getId() + "| Found "
                        + statements.size() + " special children.");
            }

        }

        return statements;

    }

    private void processNormalStatements(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected,
            String relationSubtype, List<Statement> statements)
            throws EloraException, IllegalAccessException,
            InvocationTargetException {

        if (!statements.isEmpty()) {
            boolean willChildRelationsBeProcessed = (relationSubtype.equals(
                    RELATION_SUBTYPE_HIERARCHICAL) || level == 1) ? true
                            : false;
            processStatements(level, doc, statements, cadParentStatement, false,
                    willChildRelationsBeProcessed, selected);
        }

    }

    private void processSpecialStatements(DocumentModel doc, int level,
            Statement cadParentStatement, boolean selected,
            List<Statement> statements) throws EloraException,
            IllegalAccessException, InvocationTargetException {

        if (!statements.isEmpty()) {
            if (level == 1 && isRootSpecial && isRootElement) {
                boolean willChildRelationsBeProcessed = true;
                processStatements(level, doc, statements, cadParentStatement,
                        false, willChildRelationsBeProcessed, selected);
            } else {
                boolean willChildRelationsBeProcessed = false;
                processStatements(level, doc, statements, cadParentStatement,
                        true, willChildRelationsBeProcessed, selected);
            }
        }
    }

    /**
     * Gets the child document from the statement and calls to
     * processDocumentAndRelations
     *
     * @param level
     * @param cadParent
     * @param statements
     * @param parentStatement
     * @param isSpecialAndNotRoot
     * @param willChildRelationsBeProcessed
     * @param get
     * @param checkout
     * @throws EloraException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    protected void processStatements(int level, DocumentModel cadParent,
            List<Statement> statements, Statement parentStatement,
            boolean isSpecialAndNotRoot, boolean willChildRelationsBeProcessed,
            boolean selected) throws EloraException, IllegalAccessException,
            InvocationTargetException {

        String logInitMsg = "[processStatements] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        level++;
        for (Statement statement : statements) {

            DocumentModel cadChild = null;
            try {
                cadChild = getCadChildDocument(level, parentStatement,
                        isSpecialAndNotRoot, statement);
            } catch (SpecialDocRepeatedException e) {
                // If the document is repeated, continue with the next iteration
                continue;
            }

            try {
                checkThatCadChildIsCorrect(cadChild);
            } catch (EloraException e) {
                // We ignore the children but continue processing the tree
                continue;
            }

            processDocumentAndRelations(level, false, cadChild, statement,
                    cadParent.getId(), selected, willChildRelationsBeProcessed);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private DocumentModel getCadChildDocument(int level,
            Statement parentStatement, boolean isSpecialAndNotRoot,
            Statement statement) throws SpecialDocRepeatedException {
        DocumentModel cadChild;
        if (isSpecialAndNotRoot) {
            // Don't repeat the special documents

            cadChild = EloraDocumentHelper.getDocumentModel(relationManager,
                    session, statement.getSubject());
            checkThatSpecialDocIsNotRepeated(level, statement, parentStatement,
                    cadChild);

        } else {
            cadChild = EloraDocumentHelper.getDocumentModel(relationManager,
                    session, statement.getObject());
        }
        return cadChild;
    }

    private void checkThatSpecialDocIsNotRepeated(int level,
            Statement childStatement, Statement parentStatement,
            DocumentModel cadChild) throws SpecialDocRepeatedException {
        if (level == 3) {
            if (childStatement.equals(parentStatement)) {
                throw new SpecialDocRepeatedException("Same statement.");
            }
            // Check if subject WCs are the same
            DocumentModel parentStmtSubject = EloraDocumentHelper.getDocumentModel(
                    relationManager, session, parentStatement.getSubject());
            DocumentModel cadChildWc = session.getWorkingCopy(
                    cadChild.getRef());
            DocumentModel parentStmtSubjectWc = session.getWorkingCopy(
                    parentStmtSubject.getRef());
            if (cadChildWc.getId().equals(parentStmtSubjectWc.getId())) {
                throw new SpecialDocRepeatedException("Same document.");
            }
        }

        // Don't repeat special root document when it is not an
        // initial load
        if (rootSpecialWcUid != null) {
            DocumentModel cadChildWc = session.getWorkingCopy(
                    cadChild.getRef());
            if (rootSpecialWcUid.equals(cadChildWc.getId())) {
                throw new SpecialDocRepeatedException("Same document as root.");
            }
        }
    }

    private void checkThatCadChildIsCorrect(DocumentModel cadChild)
            throws EloraException {
        if (cadChild == null) {
            throw new EloraException(
                    "The child document in the relation does not exist.");
        }

        if (cadChild.isProxy()) {
            throw new EloraException(
                    "The child document in the relation is a proxy.");
        }
        if (!cadChild.isVersion()) {
            if (!isTargetWc || action.equals("Checkout")) {
                throw new EloraException(
                        "The child document in the relation is a working copy.");
            }
        }
    }

    /**
     * @param isTargetDoc
     * @param isCadParentSelected
     * @param currentPropagationProp
     * @param cadParentStatement
     * @return
     * @throws EloraException
     */
    private PropagationProperty getPropagationProperty(boolean isTargetDoc,
            boolean isCadParentSelected,
            PropagationProperty currentPropagationProp,
            Statement cadParentStatement) throws EloraException {

        if (isTargetDoc) {
            return new PropagationProperty(isTargetSelected, isTargetEnforce);
        } else {

            // If parent is not marked to propagate, neither are its children
            if (!isCadParentSelected) {
                return new PropagationProperty();
            } else {
                return applyPropagationConfigToCurrentProp(
                        currentPropagationProp, cadParentStatement);
            }

        }

    }

    /**
     * @param currentPropagationProp
     * @param cadParentStatement
     * @return
     * @throws EloraException
     */
    private PropagationProperty applyPropagationConfigToCurrentProp(
            PropagationProperty currentPropagationProp,
            Statement cadParentStatement) throws EloraException {

        PropagationProperty updatedPropagationProp = currentPropagationProp;

        EloraConfigRow stmtPropConfig = propagationConfig.getRow(
                cadParentStatement.getPredicate().getUri());
        boolean enforce = false;
        boolean selected = false;
        if (stmtPropConfig != null) {
            enforce = ((long) stmtPropConfig.getProperty(
                    EloraConfigConstants.PROP_RELATION_PROPAGATION_ENFORCE) == 1);
            selected = ((long) stmtPropConfig.getProperty(
                    EloraConfigConstants.PROP_RELATION_PROPAGATION_PROPAGATE) == 1);
        }
        if (currentPropagationProp.getEnforce()) {
            if (enforce) {
                if (currentPropagationProp.getSelected() != selected) {
                    throw new EloraException(
                            "Contradictory propagation configuration for the same document");
                }
            }
        } else {
            updatedPropagationProp.setEnforce(enforce);
            if (enforce) {
                updatedPropagationProp.setSelected(selected);
            } else {
                updatedPropagationProp.setSelected(
                        currentPropagationProp.getSelected() || selected);
            }
        }

        return updatedPropagationProp;

    }

}
