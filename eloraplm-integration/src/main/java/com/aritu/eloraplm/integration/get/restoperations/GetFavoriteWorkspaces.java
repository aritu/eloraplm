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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
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
import org.nuxeo.ecm.platform.publisher.impl.service.DomainsFinder;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspacesResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspacesResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

@Operation(id = GetFavoriteWorkspaces.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get Favorite Workspaces", description = "Get the list of "
        + "visible workspaces for the currentUser")
public class GetFavoriteWorkspaces {

    public static final String ID = "Elora.PlmConnector.GetFavoriteWorkspaces";

    private static final Log log = LogFactory.getLog(
            GetFavoriteWorkspaces.class);

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

    protected GetWorkspacesResponse getWorkspacesResponse;

    protected static final String FAVORITE_CONTENT_PAGE_PROVIDER = "default_favorite_content";

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        getWorkspacesResponse = new GetWorkspacesResponse();

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            List<DocumentModel> domains = new DomainsFinder(
                    session.getRepositoryName()).getDomains();
            if (domains == null) {
                throw new EloraException("null domain");
            }

            TypeManager typeManager = Framework.getLocalService(
                    TypeManager.class);
            Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                    NuxeoDoctypeConstants.WORKSPACE_ROOT);
            DocumentModel favorites = favoritesManager.getFavorites(
                    domains.get(0), session);
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
            DocumentModelList favs = (DocumentModelList) service.run(subctx,
                    chain);

            for (DocumentModel fav : favs) {
                GetWorkspacesResponseDoc responseDoc = new GetWorkspacesResponseDoc(
                        fav.getId(), fav.getType(),
                        EloraStructureHelper.getStructureRootUid(fav, session),
                        fav.getTitle(), true, fav.getPathAsString());

                getWorkspacesResponse.addDocument(responseDoc);
            }

            log.trace(
                    logInitMsg + favs.size() + "| favorite workspaces found.");

            getWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getWorkspacesResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getWorkspacesResponse.setErrorMessage(e.getMessage());
            getWorkspacesResponse.emptyWorkspaces();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getWorkspacesResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getWorkspacesResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            getWorkspacesResponse.emptyWorkspaces();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        return getWorkspacesResponse.convertToJson();

    }

}
