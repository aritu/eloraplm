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
package com.aritu.eloraplm.qm.archiver;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class QmArchiverExecuters {

    private static final Log log = LogFactory.getLog(QmArchiverExecuters.class);

    public static DocumentModel replaceSubjectsWithArchivedVersion(
            DocumentModel workspace) {
        CoreSession session = workspace.getCoreSession();

        String[] subjectList = (String[]) workspace.getPropertyValue(
                EloraMetadataConstants.ELORA_QM_SUBJECT);

        List<String> avSubjectList = new ArrayList<String>();
        if (subjectList != null && subjectList.length > 0) {
            for (String subject : subjectList) {
                String avSubject = getArchivedVersionSubject(session, workspace,
                        subject);
                avSubjectList.add(avSubject);
            }

            workspace.setPropertyValue(EloraMetadataConstants.ELORA_QM_SUBJECT,
                    avSubjectList.toArray());
            workspace = session.saveDocument(workspace);
        }

        return workspace;
    }

    private static String getArchivedVersionSubject(CoreSession session,
            DocumentModel doc, String subject) {
        String logInitMsg = "[getArchivedVersionSubject] ["
                + session.getPrincipal().getName() + "] ";

        DocumentRef subjectRef = new IdRef(subject);
        if (session.exists(subjectRef)) {
            DocumentModel subjectDoc = session.getDocument(subjectRef);
            DocumentModel subjectAv;
            try {
                subjectAv = EloraDocumentHelper.getLatestVersion(subjectDoc);

                if (subjectAv != null) {
                    return subjectAv.getId();
                }
            } catch (EloraException e) {
                log.trace(logInitMsg
                        + "Could not get latest version for subject document. Will not be replaced.");
            }
        }

        return subject;
    }
}
