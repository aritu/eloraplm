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

package com.aritu.eloraplm.integration.admin.restoperations;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.archiver.api.WorkspaceArchiverService;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.admin.restoperations.util.ChangeStateAndArchiveEloraWorkspacesResponse;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * This operation changes the liceficycle state of the specified Elora
 * Workspaces to the specified final state and them it archives them. If the
 * references are already archived, their state is not modified.
 *
 * @author aritu
 *
 */
@Operation(id = ChangeStateAndArchiveEloraWorkspaces.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_ADMIN, label = "Admin - Change state and archive Elora workspaces", description = "Change the state and archive specified Elora workspaces.")
public class ChangeStateAndArchiveEloraWorkspaces {
    public static final String ID = "Elora.Admin.ChangeStateAndArchiveEloraWorkspaces";

    private static final Log log = LogFactory.getLog(
            ChangeStateAndArchiveEloraWorkspaces.class);

    @Param(name = "references", required = true)
    private List<String> references;

    @Param(name = "finalState", required = true)
    private String finalState;

    @Param(name = "skipArchiverCondition", required = false)
    private Boolean skipArchiverCondition = false;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private ChangeStateAndArchiveEloraWorkspacesResponse response;

    @OperationMethod
    public Blob run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new ChangeStateAndArchiveEloraWorkspacesResponse();

        try {

            if (!session.getPrincipal().getName().equals("Administrator")) {
                throw new EloraException(
                        "Admin operations can only be executed by administrators.");
            }

            if (references.isEmpty() || finalState.isEmpty()) {
                String error = "'references' and 'finalState' parameters cannot be empty.";
                log.error(logInitMsg + error);
                response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
                response.setErrorMessage(error);

            } else {

                // Process the references
                if (!references.isEmpty()) {
                    for (String reference : references) {
                        log.info(logInitMsg + "Processing reference |"
                                + reference + "|...");

                        processReference(reference);

                        log.info(logInitMsg + "Reference |" + reference
                                + "| processed.");
                    }

                    int refSize = references.size();
                    int errorRefSize = response.getErrorListSize();
                    int skippedRefSize = response.getSkippedListSize();
                    log.info(logInitMsg + refSize + " references processed. "
                            + (refSize - errorRefSize - skippedRefSize)
                            + " successfully processed. " + errorRefSize
                            + " in error. " + skippedRefSize + " skipped.");
                }

                response.setResult(
                        EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);
            }

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

        }

        log.trace(logInitMsg + "--- EXIT ---");

        return EloraIntegrationHelper.createJSONBlob(response.convertToJson());
    }

    private void processReference(String reference) {
        String logInitMsg = "[processReference] ["
                + session.getPrincipal().getName() + "] ";

        log.debug(logInitMsg + "Processing reference |" + reference + "|.");

        String query = EloraQueryFactory.getWcDocsByFacetAndReferenceQuery(
                EloraFacetConstants.FACET_ELORA_WORKSPACE, reference);
        DocumentModelList workspacesDml = session.query(query);

        if (workspacesDml.isEmpty()) {
            String error = "No EloraWorkspace found for reference |" + reference
                    + "|";
            log.error(logInitMsg + error);

            response.addError(reference, error);
        } else {
            for (DocumentModel workspace : workspacesDml) {
                try {

                    log.debug(logInitMsg + "Processing workspace |"
                            + workspace.getId() + "|.");

                    if (EloraDocumentHelper.isArchived(workspace)) {

                        String msg = "Workspace already archived. Id = |"
                                + workspace.getId() + "|";

                        response.addSkippedReference(reference, msg);

                    } else {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        // Change workspace lifecycle state
                        if (!workspace.getCurrentLifeCycleState().equals(
                                finalState)) {
                            String transition = LifecycleHelper.getTransitionToDestinationState(
                                    workspace, finalState);

                            if (transition == null) {
                                throw new EloraException("Transition to |"
                                        + finalState + "| is null.");
                            }

                            workspace.followTransition(transition);

                            log.info(logInitMsg
                                    + "Workspace's lifecycle state change to |"
                                    + finalState + "|");
                        }

                        // Archive workspace
                        workspace.putContextData(
                                EloraGeneralConstants.CONTEXT_SKIP_ARCHIVER_CONDITION,
                                skipArchiverCondition);
                        WorkspaceArchiverService was = Framework.getService(
                                WorkspaceArchiverService.class);
                        was.archive(workspace);

                        log.info(
                                logInitMsg + "Workspace successfuly archived.");
                    }

                } catch (Exception e) {
                    String error = "Exception processing workspace |"
                            + workspace.getId() + "|: " + e.getClass().getName()
                            + ". " + e.getMessage();

                    log.error(logInitMsg + error, e);
                    response.addError(reference, error);

                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }
            }
        }
    }

}
