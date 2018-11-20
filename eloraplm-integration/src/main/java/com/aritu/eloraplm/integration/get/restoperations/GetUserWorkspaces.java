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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.impl.service.DomainsFinder;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
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
@Operation(id = GetUserWorkspaces.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get user workspaces", description = "")
public class GetUserWorkspaces {
    public static final String ID = "Elora.PlmConnector.GetUserWorkspaces";

    private static final Log log = LogFactory.getLog(GetUserWorkspaces.class);

    protected GetUserWorkspacesResponse getUserWorkspacesResponse;

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

    /** Filter that hides HiddenInNavigation and deleted objects. */
    protected Filter documentFilter;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        getUserWorkspacesResponse = new GetUserWorkspacesResponse();

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            favoritesManager = Framework.getLocalService(
                    FavoritesManager.class);

            List<DocumentModel> domains = new DomainsFinder(
                    session.getRepositoryName()).getDomains();

            getWorkspaces(domains);

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

    protected DocumentModelList GetFavoriteWorkspaces(DocumentModel domain)
            throws OperationException {
        String FAVORITE_CONTENT_PAGE_PROVIDER = "default_favorite_content";
        TypeManager typeManager = Framework.getLocalService(TypeManager.class);
        Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                NuxeoDoctypeConstants.WORKSPACE_ROOT);
        DocumentModel favorites = favoritesManager.getFavorites(domain,
                session);
        String types = "";
        for (Type allowedType : allowedSubTypes) {
            types += "\"" + allowedType.getId() + "\",";
        }
        types = types.substring(0, types.length() - 1);
        StringList sl = new StringList();
        sl.add(types);
        sl.add("\"" + favorites.getId() + "\"");
        Map<String, Object> vars = ctx.getVars();
        vars.put("queryParams", sl);
        vars.put("providerName", FAVORITE_CONTENT_PAGE_PROVIDER);
        OperationContext subctx = new OperationContext(ctx.getCoreSession(),
                vars);
        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, vars);
        chain.add(oparams);

        return (DocumentModelList) service.run(subctx, chain);
    }

    protected void getWorkspaces(List<DocumentModel> domains)
            throws EloraException, OperationException {

        DocumentModelList favWs;

        for (DocumentModel domain : domains) {

            String wsRootQuery = EloraQueryFactory.getWorkspaceRootUidsForDomainQuery(
                    domain.getId());

            IterableQueryResult wsRootQueryResult = session.queryAndFetch(
                    wsRootQuery, NXQL.NXQL);

            List<String> wsRootUids = new ArrayList<String>();
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

            String query = EloraQueryFactory.getNotDeletedWorkspacesForWsRootsQuery(
                    wsRootUids);
            DocumentModelList workspaces = session.query(query);
            EloraStructureRootInfo structureRootInfo;
            favWs = GetFavoriteWorkspaces(domain);

            for (DocumentModel workspace : workspaces) {
                DocumentModel structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(
                        workspace, session);

                structureRootInfo = new EloraStructureRootInfo();
                structureRootInfo.setUid(structureRoot.getId());
                structureRootInfo.setTitle(structureRoot.getTitle());

                getUserWorkspacesResponse.addDocument(buildResponse(workspace,
                        structureRootInfo, favWs.contains(workspace)));

            }
        }

    }

    protected GetUserWorkspacesResponseDoc buildResponse(DocumentModel doc,
            EloraStructureRootInfo structureInfo, Boolean isFavorite) {

        String domainUid = null;
        if (!doc.getType().equals(NuxeoDoctypeConstants.DOMAIN)) {
            domainUid = doc.getParentRef().toString();
        }
        String path = doc.getPathAsString();

        EloraTypeInfo typeInfo = new EloraTypeInfo();
        typeInfo.setId(doc.getType());
        typeInfo.setLabel(EloraMessageHelper.getTranslatedMessageFromOperation(
                session, doc.getType()));

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

        return wsDoc;

    }
}
