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
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.TransitionNotAllowedException;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

/**
 * @author aritu
 *
 */
public class MakeObsoleteForBasicDocTransitionExecuter
        implements TransitionExecuter {

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
    public List<String> getErrorList() {
        // For now we don't use this method
        return new ArrayList<String>();
    }

    @Override
    public void execute(DocumentModel doc)
            throws EloraException, TransitionNotAllowedException {

        String transition = EloraLifeCycleConstants.TRANS_OBSOLETE;

        CoreSession session = doc.getCoreSession();

        // Check if parents allow transition
        List<Resource> predicateList = new ArrayList<Resource>();
        predicateList.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));
        predicateList.add(
                new ResourceImpl(EloraRelationConstants.CAD_HAS_DOCUMENT));
        if (!PromoteHelper.parentsAllowTransition(doc, transition,
                predicateList)) {
            throw new TransitionNotAllowedException(doc, transition);

        }

        if (doc.isImmutable()) {

            LifecycleHelper.followTransitionAndLaunchEvent(doc, transition,
                    PdmEventNames.PDM_PROMOTED_EVENT, doc.getVersionLabel());

        } else {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc == null) {
                throw new EloraException("The document |" + doc.getId()
                        + "| has no base version.");
            }

            LifecycleHelper.followTransitionAndLaunchEvent(baseDoc, transition,
                    PdmEventNames.PDM_PROMOTED_EVENT, doc.getVersionLabel());

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

}
