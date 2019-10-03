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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.platform.publisher.impl.service.DomainsFinder;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author aritu
 */
@Operation(id = GetNavigationFolder.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get navigation folder", description = "")
public class GetNavigationFolder {

    public static final String ID = "Elora.PlmConnector.GetNavigationFolder";

    private static final Log log = LogFactory.getLog(GetNavigationFolder.class);

    @Context
    protected CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "wcUid", required = false)
    protected DocumentRef wcRef;

    protected Filter documentFilter;

    protected GetResponse getResponse;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            getResponse = new GetResponse();
            documentFilter = EloraDocumentHelper.getDocumentFilter();

            if (wcRef == null) {
                getDomains();
            } else {
                DocumentModel doc = session.getDocument(wcRef);
                switch (doc.getType()) {
                case NuxeoDoctypeConstants.DOMAIN:
                    getStructRoots();
                    break;
                case EloraDoctypeConstants.STRUCTURE_ROOT:
                    getWorkspaceRoot();
                    break;
                case NuxeoDoctypeConstants.WORKSPACE_ROOT:
                    getWorkspaces();
                    break;
                default:
                    throw new EloraException("wcUid |" + wcRef.toString()
                            + "| is not a valid document type");
                }
            }
            getResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getResponse.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
        }
        log.trace(logInitMsg + "--- EXIT --- ");
        return getResponse.convertToJson();
    }

    protected void getDomains() throws EloraException {
        String logInitMsg = "[getDomains] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<DocumentModel> domains = new DomainsFinder(
                session.getRepositoryName()).getDomains();
        if (domains == null) {
            throw new EloraException("null domain");
        }

        for (DocumentModel domain : domains) {
            buildResponse(domain);
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected void getStructRoots() throws EloraException {
        String logInitMsg = "[getStructRoots] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // If perm is not passed nuxeo checks READ permission by default
        DocumentModelList structRoots = session.getChildren(wcRef,
                EloraDoctypeConstants.STRUCTURE_ROOT, documentFilter, null);

        for (DocumentModel structRoot : structRoots) {
            buildResponse(structRoot);
        }
        log.trace(logInitMsg + structRoots.size() + "| structureRoots found.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected void getWorkspaceRoot() throws EloraException {
        String logInitMsg = "[getWorkspaceRoot] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // If perm is not passed nuxeo checks READ permission by default
        DocumentModelList workspaceRoots = session.getChildren(wcRef,
                NuxeoDoctypeConstants.WORKSPACE_ROOT, documentFilter, null);

        for (DocumentModel workspaceRoot : workspaceRoots) {
            buildResponse(workspaceRoot);
        }
        log.trace(
                logInitMsg + workspaceRoots.size() + "| workspaceRoots found.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected void getWorkspaces() throws EloraException {
        String logInitMsg = "[getWorkspaces] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // If perm is not passed nuxeo checks READ permission by default
        DocumentModelList workspaces = session.getChildren(wcRef,
                NuxeoDoctypeConstants.WORKSPACE, documentFilter, null);

        for (DocumentModel workspace : workspaces) {
            buildResponse(workspace);
        }
        log.trace(logInitMsg + workspaces.size() + "| workspaces found.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    // TODO: si es posible unir con GetDomains, GetUnes, GetWorkspaces
    protected void buildResponse(DocumentModel doc) {

        String logInitMsg = "[buildResponse] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String realUid = null;
        String wcUid = doc.getId();
        String title = doc.getTitle();
        String type = doc.getType();
        String parentRealUid = null;
        if (!doc.getType().equals(NuxeoDoctypeConstants.DOMAIN)) {
            parentRealUid = doc.getParentRef().toString();
        }
        String path = doc.getPathAsString();

        GetResponseDoc responseDoc = new GetResponseDoc(wcUid, realUid, title,
                type, parentRealUid, path);

        getResponse.addDocument(responseDoc);
    }

}
