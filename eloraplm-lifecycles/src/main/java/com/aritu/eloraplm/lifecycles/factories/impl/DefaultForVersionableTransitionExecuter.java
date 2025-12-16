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
package com.aritu.eloraplm.lifecycles.factories.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.lifecycles.api.LifecycleConfigService;
import com.aritu.eloraplm.core.lifecycles.api.TransitionDescriptor;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.util.ErrorInfo;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;

/**
 * @author aritu
 *
 */
public class DefaultForVersionableTransitionExecuter
        implements TransitionExecuter {

    private String lifecycle;

    private String transition;

    private String direction;

    public DefaultForVersionableTransitionExecuter(String lifecycle,
            String transition) {
        this.lifecycle = lifecycle;
        this.transition = transition;
        LifecycleConfigService lcs = Framework.getService(
                LifecycleConfigService.class);
        try {
            TransitionDescriptor td = lcs.getTransitionConfig(lifecycle,
                    transition);
            direction = td.direction;
        } catch (EloraException e) {
            // TODO hemen zer
            throw new RuntimeException("Could not get transition config.");
        }
    }

    @Override
    public String getPreviousScreen() {
        return null;
    }

    @Override
    public void init(DocumentModel doc) {
    }

    @Override
    public boolean canBeExecuted() {
        return true;
    }

    @Override
    public List<ErrorInfo> getErrorList() {
        // For now we don't use this method
        return new ArrayList<ErrorInfo>();
    }

    @Override
    public void execute(DocumentModel doc) throws EloraException {

        CoreSession session = doc.getCoreSession();

        String eventName = null;
        if (direction.equals(
                LifecycleConfigService.TRANSITION_DIRECTION_PROMOTE)) {
            eventName = PdmEventNames.PDM_PROMOTED_EVENT;
        } else if (direction.equals(
                LifecycleConfigService.TRANSITION_DIRECTION_DEMOTE)) {
            eventName = PdmEventNames.PDM_DEMOTED_EVENT;
        }

        if (doc.isImmutable()) {

            LifecycleHelper.followTransitionAndLaunchEvent(doc, transition,
                    eventName, doc.getVersionLabel());

            // Fire Approved event
            if (transition.equals(EloraLifeCycleConstants.TRANS_APPROVE)) {
                EloraEventHelper.fireEvent(PdmEventNames.PDM_APPROVED_EVENT,
                        doc);
            }

        } else {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc == null) {
                throw new EloraException("The document |" + doc.getId()
                        + "| has no base version.");
            }

            LifecycleHelper.followTransitionAndLaunchEvent(baseDoc, transition,
                    eventName, doc.getVersionLabel());

            // Fire Approved event
            if (transition.equals(EloraLifeCycleConstants.TRANS_APPROVE)) {
                EloraEventHelper.fireEvent(PdmEventNames.PDM_APPROVED_EVENT,
                        baseDoc);
            }

            // We cannot follow transition instead of restoring, because it
            // checks the document out always. This is the only way we know to
            // change the state without checkin the document out.
            EloraDocumentHelper.restoreToVersion(doc.getRef(), baseDoc.getRef(),
                    true, true, session);
        }

    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return false;
    }

    @Override
    public boolean hasInnerForm() {
        return false;
    }

}
