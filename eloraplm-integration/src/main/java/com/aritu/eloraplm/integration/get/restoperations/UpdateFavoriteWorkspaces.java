/**
 *
 */

package com.aritu.eloraplm.integration.get.restoperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.publisher.impl.service.DomainsFinder;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspacesResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspacesResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

@Operation(id = UpdateFavoriteWorkspaces.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Update Favorite Workspaces", description = "")
public class UpdateFavoriteWorkspaces {

    public static final String ID = "Elora.PlmConnector.UpdateFavoriteWorkspaces";

    private static final Log log = LogFactory.getLog(
            UpdateFavoriteWorkspaces.class);

    // TODO: Agrupar en una clase
    protected static final String FAVORITE_CONTENT_PAGE_PROVIDER = "default_favorite_content";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Context
    protected PageProviderService ppService;

    protected GetWorkspacesResponse getWorkspacesResponse;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "workspaces", required = true)
    public ArrayList<JsonNode> favoriteDocs;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        getWorkspacesResponse = new GetWorkspacesResponse();

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            FavoritesManager favoritesManager = Framework.getLocalService(
                    FavoritesManager.class);
            List<DocumentModel> domains = new DomainsFinder(
                    session.getRepositoryName()).getDomains();
            if (domains == null) {
                throw new EloraException("null domain");
            }
            // TODO: Es posible que favorites devuelva mas de un valor
            DocumentModel favorites = favoritesManager.getFavorites(
                    domains.get(0), session);
            TypeManager typeManager = Framework.getLocalService(
                    TypeManager.class);
            Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                    NuxeoDoctypeConstants.WORKSPACE_ROOT);
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
            DocumentModelList currentFavorites = (DocumentModelList) service.run(
                    subctx, chain);

            DocumentModelList newFavorites = new DocumentModelListImpl();
            // Add new selected favorites
            for (int i = 0; i < favoriteDocs.size(); ++i) {
                JsonNode favoriteDoc = favoriteDocs.get(i);

                DocumentRef docRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                        favoriteDoc, "realUid", false);
                DocumentModel doc = session.getDocument(docRef);
                if (!currentFavorites.contains(doc)) {
                    favoritesManager.addToFavorites(doc, session);
                }
                newFavorites.add(doc);
            }
            // Remove not selected favorites from actual favorites list
            for (DocumentModel currentFavorite : currentFavorites) {
                if (newFavorites.isEmpty()
                        || !newFavorites.contains(currentFavorite)) {
                    favoritesManager.removeFromFavorites(currentFavorite,
                            session);
                }
            }

            for (DocumentModel favorite : newFavorites) {
                getWorkspacesResponse.addDocument(new GetWorkspacesResponseDoc(
                        favorite.getId(), favorite.getType(),
                        EloraStructureHelper.getStructureRootUid(favorite,
                                session),
                        favorite.getTitle(), true, favorite.getPathAsString()));
            }

            log.trace(logInitMsg + newFavorites.size() + "| favorites added.");

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

        log.trace(logInitMsg + "--- EXIT --- ");

        return getWorkspacesResponse.convertToJson();
    }
}
