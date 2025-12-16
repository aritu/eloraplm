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
package com.aritu.eloraplm.doctypes.datatable;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.datatable.EditableDocBasedTable;
import com.aritu.eloraplm.datatable.RowData;

/**
 *
 * @author aritu
 *
 */
@Name(DocumentVersionsTableBean.NAME)
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class DocumentVersionsTableBean extends EditableDocBasedTable {

    public static final String NAME = "documentVersionsTableBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            DocumentVersionsTableBean.class);

    /* Add/Edit fields */

    private String type;

    private String reference;

    private String version;

    private String sourceVersion;

    private String comment;

    private String lifecycleState;

    private String lastContributor;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    /* ---------------- */

    public DocumentVersionsTableBean() {
        tableService = new DocumentVersionsTableServiceImpl();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(getCurrentDocument()));
            setIsDirty(false);
            log.trace(logInitMsg + "Table created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (getIsDirty()) {
            try {

                boolean updateNextVersions = false;
                for (RowData r : getData()) {
                    DocumentVersionRowData row = (DocumentVersionRowData) r;
                    if (!row.getIsRemoved()
                            && (row.getIsModified() || updateNextVersions)) {

                        updateNextVersions = true;

                        DocumentModel versionDoc = row.getVersionDoc();

                        String previousComment = versionDoc.getCheckinComment();

                        EloraDocumentHelper.updateCheckinComment(versionDoc,
                                row.getComment());

                        // Nuxeo Event
                        String comment = "'" + previousComment + "' => '"
                                + row.getComment() + "'";
                        EloraEventHelper.fireEvent(
                                EloraEventNames.ELORA_VERSION_COMMENT_CHANGED_EVENT,
                                versionDoc, comment);

                        row.setIsModified(false);

                        versionDoc.refresh();

                        EloraEventHelper.fireEvent(
                                PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT,
                                versionDoc, comment);

                        EloraDocumentHelper.disableVersioningDocument(
                                versionDoc);

                        documentManager.saveDocument(versionDoc);

                        if (EloraDocumentHelper.isBaseVersion(versionDoc)) {
                            DocumentModel wcDoc = documentManager.getWorkingCopy(
                                    versionDoc.getRef());
                            if (!wcDoc.isCheckedOut()) {
                                wcDoc = EloraDocumentHelper.restoreToVersion(
                                        wcDoc.getRef(), versionDoc.getRef(),
                                        true, true, documentManager);
                            }
                        }
                    }
                }

                createData();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.history.changeCheckinComments"));
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.success.history.changeCheckinComments"));
            }
        }
    }
}
