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
package com.aritu.eloraplm.integration.restoperations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.restoperations.util.SyncVocabularyResponse;
import com.aritu.eloraplm.integration.restoperations.util.VocabularyContentFactory;
import com.aritu.eloraplm.integration.restoperations.util.VocabularyInfo;

/**
 * Operation to check session status
 *
 * @author aritu
 *
 */
@Operation(id = SyncVocabulary.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - SyncVocabulary", description = "Sync vocabulary data if it is not up-to-date.")
public class SyncVocabulary {

    public static final String ID = "Elora.PlmConnector.SyncVocabulary";

    private static final Log log = LogFactory.getLog(SyncVocabulary.class);

    @Param(name = "vocabulary", required = true)
    protected JsonNode vocabulary;

    @Context
    protected CoreSession session;

    protected String vocId;

    protected String vocTimestamp;

    protected SyncVocabularyResponse syncResponse;

    protected String serverTimestamp;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        syncResponse = new SyncVocabularyResponse();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            loadInputData();
            getServerTimestamp();

            VocabularyInfo vocInfo = syncResponse.getVocabulary();
            vocInfo.setId(vocId);
            vocInfo.setTimestamp(serverTimestamp);
            vocInfo.setUpdate(false);

            if (!isTimestampUpToDate()) {
                vocInfo.setUpdate(true);

                EloraConfigTable configTable = EloraConfigHelper.getConfigTable(
                        vocId);
                vocInfo.setContent(VocabularyContentFactory.convertConfigTable(
                        vocId, configTable));
            }

            log.info(logInitMsg + "Document successfuly saved.");
            syncResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            syncResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            syncResponse.setErrorMessage(e.getMessage());

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            syncResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            syncResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = syncResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    protected void loadInputData() throws EloraException {
        String logInitMsg = "[loadInputData] ["
                + session.getPrincipal().getName() + "] ";

        vocId = EloraJsonHelper.getJsonFieldAsString(vocabulary, "id", true);
        vocTimestamp = EloraJsonHelper.getJsonFieldAsString(vocabulary,
                "timestamp", true);

        log.trace(logInitMsg + " Vocabulary " + vocId + " has timestamp "
                + vocTimestamp + ".");
    }

    protected void getServerTimestamp() throws EloraException {
        serverTimestamp = EloraConfigHelper.getVocabularyTimestamp(vocId);
    }

    protected boolean isTimestampUpToDate() {
        return serverTimestamp.equals(vocTimestamp);

    }
}
