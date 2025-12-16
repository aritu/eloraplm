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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.core.archiver.api.WorkspaceArchiverService;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.ArchivingConditionsNotMetException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.util.ErrorInfo;

/**
 * @author aritu
 *
 */
public class ArchiveForWorkspaceTransitionExecuter
        implements TransitionExecuter {

    private static final Log log = LogFactory.getLog(
            ArchiveForWorkspaceTransitionExecuter.class);

    private String transition;

    public ArchiveForWorkspaceTransitionExecuter(String transition) {
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
    public List<ErrorInfo> getErrorList() {
        // For now we don't use this method
        return new ArrayList<ErrorInfo>();
    }

    @Override
    public void execute(DocumentModel doc) {
        String logInitMsg = "[execute] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        FacesMessages facesMessages = FacesMessages.instance();

        try {

            // follow given transition
            doc.followTransition(transition);

            // archive document
            WorkspaceArchiverService was = Framework.getService(
                    WorkspaceArchiverService.class);

            was.archive(doc);

            if (facesMessages != null) {
                facesMessages.add(StatusMessage.Severity.INFO,
                        EloraMessageHelper.getTranslatedMessage(
                                "eloraplm.message.success.archive.workspace"));
            }

        } catch (NuxeoException | ArchivingConditionsNotMetException
                | EloraException e) {

            log.error(logInitMsg + e.getMessage(), e);

            if (facesMessages != null) {
                facesMessages.add(StatusMessage.Severity.INFO,
                        EloraMessageHelper.getTranslatedMessage(
                                "eloraplm.message.error.promoteDemote.archiveworkspace"));
            }
        }
    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return true;
    }

    @Override
    public boolean hasInnerForm() {
        return false;
    }

}
