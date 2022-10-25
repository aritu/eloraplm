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
package com.aritu.eloraplm.cm.batchProcessing;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import com.aritu.eloraplm.cm.batchProcessing.util.BatchProcessInfo;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;

@Name("cmAsyncBatchProcessInfo")
@Scope(ScopeType.APPLICATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmAsyncBatchProcessInfoBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmAsyncBatchProcessInfoBean.class);

    private Hashtable<String, BatchProcessInfo> cmProcessesInProgress = new Hashtable<String, BatchProcessInfo>();

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @Observer(value = { CMBatchProcessingEventNames.IN_PROGRESS })
    @BypassInterceptors
    public void markBatchProcessAsInProgress(String cmProcessDocId,
            String itemType, String itemClass, String processingAction,
            int totalCount) {
        String logInitMsg = "[markBatchProcessAsInProgress] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcessDocId = |" + cmProcessDocId
                + "|, itemType = |" + itemType + "|, totalCount =  |"
                + totalCount + "|");

        if (!cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = new BatchProcessInfo(itemType,
                    itemClass, processingAction, totalCount, 0, true, 1);
            cmProcessesInProgress.put(cmProcessDocId, processInfo);
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Checks if there is any batch process running
     *
     * @param cmProcessDocId
     * @return
     */
    public boolean isBatchProcessInProgress(String cmProcessDocId) {
        boolean isInProgress = false;

        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = cmProcessesInProgress.get(
                    cmProcessDocId);
            isInProgress = processInfo.getIsInProgress();
        }

        return isInProgress;
    }

    /**
     * Checks if batch process info needs to be refreshed in screen (ajax call)
     *
     * @param cmProcessDocId
     * @return
     */
    public boolean needToRefreshBatchProcessInfo(String cmProcessDocId) {
        boolean needToBeRefreshed = false;

        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = cmProcessesInProgress.get(
                    cmProcessDocId);

            if (processInfo.getIsInProgress()) {
                needToBeRefreshed = true;
            } else {
                int needToBeRefreshedInt = processInfo.getNeedToBeRefreshed();
                needToBeRefreshedInt--;
                processInfo.setNeedToBeRefreshed(needToBeRefreshedInt);

                cmProcessesInProgress.put(cmProcessDocId, processInfo);

                if (needToBeRefreshedInt > -1) {
                    needToBeRefreshed = true;
                }
            }
        }

        return needToBeRefreshed;
    }

    public BatchProcessInfo getBatchProcessInfo(String cmProcessDocId) {
        BatchProcessInfo processInfo = null;

        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            processInfo = cmProcessesInProgress.get(cmProcessDocId);
        }
        return processInfo;
    }

    @Observer(value = {
            CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER })
    @BypassInterceptors
    public void increaseBatchProcessCounter(String cmProcessDocId) {
        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = cmProcessesInProgress.get(
                    cmProcessDocId);
            processInfo.increaseProcessCounter();
            cmProcessesInProgress.put(cmProcessDocId, processInfo);

        }
    }

    @Observer(value = { CMBatchProcessingEventNames.RESET_PROCESSED_COUNTER })
    @BypassInterceptors
    public void resetProcessedCountAndChangeAction(String cmProcessDocId,
            String processingAction) {
        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = cmProcessesInProgress.get(
                    cmProcessDocId);
            processInfo.setProcessingAction(processingAction);
            processInfo.setProcessedCount(0);
            cmProcessesInProgress.put(cmProcessDocId, processInfo);
        }
    }

    @Observer(value = { CMBatchProcessingEventNames.FINISHED })
    @BypassInterceptors
    public void stopRefreshingBatchProcessInfo(String cmProcessDocId) {
        String logInitMsg = "[finishProcess] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcessDocId = |" + cmProcessDocId + "|");

        if (cmProcessesInProgress.containsKey(cmProcessDocId)) {
            BatchProcessInfo processInfo = cmProcessesInProgress.get(
                    cmProcessDocId);
            processInfo.setIsInProgress(false);
            cmProcessesInProgress.put(cmProcessDocId, processInfo);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    @Observer(value = { CMBatchProcessingEventNames.REFRESHED })
    @BypassInterceptors
    public void removeBatchProcessInfo(String cmProcessDocId) {
        String logInitMsg = "[removeBatchProcessInfo] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcessDocId = |" + cmProcessDocId + "|");

        cmProcessesInProgress.remove(cmProcessDocId);

        log.trace(logInitMsg + "--- EXIT --- ");
    }
}