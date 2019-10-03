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
package com.aritu.eloraplm.lifecycles.util;

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for life cycles stuff.
 *
 * @author aritu
 *
 */
public class LifecycleHelper {

    private static final Log log = LogFactory.getLog(LifecycleHelper.class);

    /**
     * Returns the transition name for moving the document to the given
     * destination state.
     *
     * @param doc
     * @param destinationStateName
     * @return
     * @throws EloraException
     */
    public static String getTransitionToDestinationState(DocumentModel doc,
            String destinationStateName) throws EloraException {
        String transitionName = null;
        String logInitMsg = "[getTransitionToDestinationState] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            if (doc == null) {
                String msg = "Specified doc is null";
                log.error(logInitMsg + msg);
                throw new EloraException(msg);
            }

            logInitMsg += "[" + doc.getCoreSession().getPrincipal().getName()
                    + "]";

            if (destinationStateName == null
                    || destinationStateName.length() == 0) {
                String msg = "Specified destinationStateName is null";
                log.error(logInitMsg + msg);
                throw new EloraException(msg);
            }

            Collection<String> currentAllowedTransitionNames = doc.getAllowedStateTransitions();
            String lifeCyclePolicy = doc.getLifeCyclePolicy();

            LifeCycleService service = NXCore.getLifeCycleService();
            LifeCycle lifeCycle = service.getLifeCycleByName(lifeCyclePolicy);

            Collection<LifeCycleTransition> lifecycleTransitions = lifeCycle.getTransitions();

            if (lifecycleTransitions != null
                    && lifecycleTransitions.size() > 0) {
                for (Iterator<LifeCycleTransition> iterator = lifecycleTransitions.iterator(); iterator.hasNext();) {
                    LifeCycleTransition lifeCycleTransition = iterator.next();
                    if (currentAllowedTransitionNames.contains(
                            lifeCycleTransition.getName())
                            && lifeCycleTransition.getDestinationStateName().equals(
                                    destinationStateName)) {
                        transitionName = lifeCycleTransition.getName();
                        break;
                    }
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- with transitionName = |"
                + transitionName + "|");

        return transitionName;
    }
}
