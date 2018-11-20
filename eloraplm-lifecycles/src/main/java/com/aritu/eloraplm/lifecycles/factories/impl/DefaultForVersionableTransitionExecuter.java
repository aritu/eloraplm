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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;

/**
 * @author aritu
 *
 */
public class DefaultForVersionableTransitionExecuter
        implements TransitionExecuter {

    private String transition;

    public DefaultForVersionableTransitionExecuter(String transition) {
        this.transition = transition;
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
    public void execute(DocumentModel doc) {

        CoreSession session = doc.getCoreSession();

        if (doc.isImmutable()) {
            doc.followTransition(transition);
        } else {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            baseDoc.followTransition(transition);

            // We cannot follow transition instead of restoring, because it
            // checks the document out always. This is the only way we know to
            // change the state without checkin the document out.
            EloraDocumentHelper.restoreToVersion(doc.getRef(), baseDoc.getRef(),
                    true, true, session);
        }

    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return true;
    }

}
