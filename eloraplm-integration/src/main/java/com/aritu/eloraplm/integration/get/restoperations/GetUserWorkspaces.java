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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.impl.service.DomainsFinder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.IntegrationConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetUserWorkspacesRequestFilters;
import com.aritu.eloraplm.integration.get.restoperations.util.GetUserWorkspacesResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetUserWorkspacesResponseDoc;
import com.aritu.eloraplm.integration.restoperations.util.EloraStructureRootInfo;
import com.aritu.eloraplm.integration.restoperations.util.EloraTypeInfo;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
@Operation(id = GetUserWorkspaces.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get user workspaces", description = "")
public class GetUserWorkspaces {
    public static final String ID = "Elora.PlmConnector.GetUserWorkspaces";

    private static final Log log = LogFactory.getLog(GetUserWorkspaces.class);

    protected GetUserWorkspacesResponse getUserWorkspacesResponse;

    protected GetUserWorkspacesRequestFilters requestFilters;

    @Context
    protected CoreSession session;

    @Context
    protected FavoritesManager favoritesManager;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "filters", required = false)
    private ArrayList<JsonNode> filters;

    /** Filter that hides HiddenInNavigation and deleted objects. */
    protected Filter documentFilter;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        getUserWorkspacesResponse = new GetUserWorkspacesResponse();
        requestFilters = new GetUserWorkspacesRequestFilters();

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            // If filters have been specified, load them
            if (filters != null) {
                loadRequestFilters();
            }

            favoritesManager = Framework.getLocalService(
                    FavoritesManager.class);

            getWorkspaces(requestFilters);

            getUserWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getUserWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getUserWorkspacesResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getUserWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getUserWorkspacesResponse.setErrorMessage(e.getMessage());

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getUserWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getUserWorkspacesResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        return getUserWorkspacesResponse.convertToJson();
    }

    private void loadRequestFilters() throws EloraException {
        String logInitMsg = "[loadRequestFilters] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + filters.size()
                + " filter(s) specified as entry parameter.");

        for (int i = 0; i < filters.size(); ++i) {

            JsonNode filterItem = filters.get(i);
            String fieldName = EloraJsonHelper.getJsonFieldAsString(filterItem,
                    "field", true);
            switch (fieldName) {
            case IntegrationConstants.REQUEST_FILTER_LIFE_CYCLE_STATE:
                String lifeCycleState = EloraJsonHelper.getJsonFieldAsString(
                        filterItem, "value", true);
                requestFilters.setLifeCycleState(lifeCycleState);
                break;
            case IntegrationConstants.REQUEST_FILTER_TYPE:
                String type = EloraJsonHelper.getJsonFieldAsString(filterItem,
                        "value", true);
                requestFilters.setType(type);
                break;
            case IntegrationConstants.REQUEST_FILTER_STRUCTURE_ROOT:
                String structureRoot = EloraJsonHelper.getJsonFieldAsString(
                        filterItem, "value", true);
                requestFilters.setStructureRoot(structureRoot);
                break;
            case IntegrationConstants.REQUEST_FILTER_ONLY_FAVORITE:
                boolean onlyFavorite = EloraJsonHelper.getJsonFieldAsBoolean(
                        filterItem, "value", true);
                requestFilters.setOnlyFavorite(onlyFavorite);
                break;
            default:
                break;
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected void getWorkspaces(GetUserWorkspacesRequestFilters requestFilters)
            throws EloraException, OperationException {

        DocumentModelList workspaces = new DocumentModelListImpl();

        // Retrieve the wsRootUids list in function of specified structureRoot
        // filter:
        // - if a structureRoot is defined, the ancestor for retrieving the
        // wsRootUids list is the structureRoot itself
        // - otherwise, we should calculate the wsRootUids regarding all
        // available domains
        List<String> wsRootUids = new ArrayList<String>();

        if (requestFilters.getStructureRoot() != null
                && !requestFilters.getStructureRoot().isEmpty()) {
            wsRootUids = retrieveWorkspaceRootListByAncestor(
                    requestFilters.getStructureRoot());

        } else {
            // if not any structure root has been defined, we should start
            // looking from user domains.
            List<DocumentModel> domains = new DomainsFinder(
                    session.getRepositoryName()).getDomains();

            for (DocumentModel domain : domains) {
                wsRootUids.addAll(
                        retrieveWorkspaceRootListByAncestor(domain.getId()));
            }
        }

        // Construct the query in function of lifeCycleState and type filters
        String query = "";
        if (requestFilters.getLifeCycleState() != null
                && !requestFilters.getLifeCycleState().isEmpty()) {
            if (requestFilters.getType() != null
                    && !requestFilters.getType().isEmpty()) {

                query = EloraQueryFactory.getWorkspacesForWsRootsLifeCycleStateAndTypeQuery(
                        wsRootUids, requestFilters.getLifeCycleState(),
                        requestFilters.getType());
            } else {
                query = EloraQueryFactory.getWorkspacesForWsRootsAndLifeCycleStateQuery(
                        wsRootUids, requestFilters.getLifeCycleState());
            }
        } else {
            if (requestFilters.getType() != null
                    && !requestFilters.getType().isEmpty()) {
                query = EloraQueryFactory.getNotDeletedWorkspacesForWsRootsAndTypeQuery(
                        wsRootUids, requestFilters.getType());
            } else {
                query = EloraQueryFactory.getNotDeletedWorkspacesForWsRootsQuery(
                        wsRootUids);
            }
        }
        workspaces = session.query(query);

        // Add found workspaces to the response
        if (workspaces != null) {

            for (DocumentModel workspace : workspaces) {

                // if filter onlyFavorite is true, return only favorite one
                boolean isFavorite = favoritesManager.isFavorite(workspace,
                        session);
                if (!requestFilters.isOnlyFavorite()
                        || (requestFilters.isOnlyFavorite() && isFavorite)) {
                    DocumentModel structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(
                            workspace, session);
                    EloraStructureRootInfo structureRootInfo = new EloraStructureRootInfo();
                    structureRootInfo.setUid(structureRoot.getId());
                    structureRootInfo.setTitle(structureRoot.getTitle());

                    getUserWorkspacesResponse.addDocument(buildResponse(
                            workspace, structureRootInfo, isFavorite));
                }
            }
        }
    }

    private List<String> retrieveWorkspaceRootListByAncestor(
            String ancestorUid) {
        List<String> wsRootUids = new ArrayList<String>();
        String wsRootQuery = EloraQueryFactory.getWorkspaceRootUidsByAncestorQuery(
                ancestorUid);
        IterableQueryResult wsRootQueryResult = session.queryAndFetch(
                wsRootQuery, NXQL.NXQL);
        try {
            if (wsRootQueryResult.size() > 0) {
                for (Map<String, Serializable> map : wsRootQueryResult) {
                    String uid = (String) map.get(NXQL.ECM_UUID);
                    wsRootUids.add(uid);
                }
            }
        } finally {
            wsRootQueryResult.close();
        }
        return wsRootUids;
    }

    protected GetUserWorkspacesResponseDoc buildResponse(DocumentModel doc,
            EloraStructureRootInfo structureInfo, boolean isFavorite) {

        String domainUid = null;
        if (!doc.getType().equals(NuxeoDoctypeConstants.DOMAIN)) {
            domainUid = doc.getParentRef().toString();
        }
        String path = doc.getPathAsString();

        EloraTypeInfo typeInfo = new EloraTypeInfo();
        typeInfo.setId(doc.getType());
        typeInfo.setLabel(EloraMessageHelper.getTranslatedMessage(session,
                doc.getType()));

        GetUserWorkspacesResponseDoc wsDoc = new GetUserWorkspacesResponseDoc();
        wsDoc.setDomainRealUid(domainUid);
        wsDoc.setRealUid(doc.getId());
        Serializable reference = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        if (reference != null) {
            wsDoc.setReference(reference.toString());
        }
        wsDoc.setTypeInfo(typeInfo);
        wsDoc.setTitle(doc.getTitle());
        wsDoc.setPath(path);
        wsDoc.setStructureRootInfo(structureInfo);
        wsDoc.setIsFavorite(isFavorite);
        wsDoc.setLifecycleState(doc.getCurrentLifeCycleState());
        Serializable description = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION);
        if (description != null) {
            wsDoc.setDescription(description.toString());
        }

        return wsDoc;
    }
}
