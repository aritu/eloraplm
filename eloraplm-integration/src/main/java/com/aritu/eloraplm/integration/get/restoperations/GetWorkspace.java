///*
// * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// */
//
package com.aritu.eloraplm.integration.get.restoperations;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoader;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoaderFactory;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspaceResponse;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

@Operation(id = GetWorkspace.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get Workspace", description = "Get workspace structure.")
public class GetWorkspace {
    public static final String ID = "Elora.PlmConnector.GetWorkspace";

    private static final Log log = LogFactory.getLog(GetWorkspace.class);

    private static final String CHILDREN_VERSIONS_AS_STORED = "AsStored";

    private static final String CHILDREN_VERSIONS_LATEST_VERSIONS = "LatestVersions";

    private static final String CHILDREN_VERSIONS_LATEST_RELEASED = "LatestReleased";

    private static final String[] CHILDREN_VERSIONS = {
            CHILDREN_VERSIONS_AS_STORED, CHILDREN_VERSIONS_LATEST_VERSIONS,
            CHILDREN_VERSIONS_LATEST_RELEASED };

    @Context
    protected OperationContext ctx;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "wcUid", required = true)
    protected DocumentRef wcRef;

    @Param(name = "contentChildrenVersions", required = true)
    private String contentChildrenVersions;

    @Param(name = "cmProcessRootItemChildrenVersions", required = false)
    private String cmProcessRootItemChildrenVersions;

    @Param(name = "cmProcessSubitemChildrenVersions", required = false)
    private String cmProcessSubitemChildrenVersions;

    @Context
    protected CoreSession session;

    protected DocumentModel wsDoc;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        GetWorkspaceResponse response = new GetWorkspaceResponse();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            checkIfChildrenVersionsOptionsAreAllowed();

            wsDoc = session.getDocument(wcRef);

            WorkspaceDataLoaderFactory factory = Framework.getService(
                    WorkspaceDataLoaderFactory.class);
            WorkspaceDataLoader dl = factory.getDataLoader(wsDoc);
            response = dl.getDataAndCreateResponse(
                    (HttpServletRequest) ctx.get("request"),
                    contentChildrenVersions, cmProcessRootItemChildrenVersions,
                    cmProcessSubitemChildrenVersions);

            response.setLastModified(getLastModified());

            log.info("Workspace successfuly processed.");

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());
            response.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            response.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = response.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    private Date getLastModified() {
        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) wsDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            lastModifiedGc.set(Calendar.MILLISECOND, 0);
            lastModified = lastModifiedGc.getTime();
        }

        return lastModified;
    }

    private void checkIfChildrenVersionsOptionsAreAllowed()
            throws EloraException {
        if (!Arrays.asList(CHILDREN_VERSIONS).contains(
                contentChildrenVersions)) {

            throw new EloraException(
                    "The content children versions option must be "
                            + String.join(" or ",
                                    Arrays.asList(CHILDREN_VERSIONS))
                            + ".");
        }
        if (cmProcessRootItemChildrenVersions != null
                && !Arrays.asList(CHILDREN_VERSIONS).contains(
                        cmProcessRootItemChildrenVersions)) {

            throw new EloraException(
                    "The root items children versions option must be "
                            + String.join(" or ",
                                    Arrays.asList(CHILDREN_VERSIONS))
                            + ".");
        }
        if (cmProcessSubitemChildrenVersions != null
                && !Arrays.asList(CHILDREN_VERSIONS).contains(
                        cmProcessSubitemChildrenVersions)) {

            throw new EloraException(
                    "The subitems children versions option must be "
                            + String.join(" or ",
                                    Arrays.asList(CHILDREN_VERSIONS))
                            + ".");
        }
    }

}
