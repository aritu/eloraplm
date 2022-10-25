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
package com.aritu.eloraplm.om.actionbeans;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.OmEventNames;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraUtil;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * CM actions bean.
 *
 * @author aritu
 *
 */
@Name("omActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class OmActionsBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In
    protected transient NavigationContext navigationContext;

    private static final Log log = LogFactory.getLog(OmActionsBean.class);

    private String sourceDocWcUid;

    private String sourceDocRealUid;

    private boolean isSourceInEditMode = true;

    private boolean includeAllPreviousVersions = false;

    private Map<String, String> sourceDocVersionList;

    public OmActionsBean() {
        sourceDocVersionList = new HashMap<String, String>();
    }

    // Getters & Setters

    public boolean getIsSourceInEditMode() {
        return isSourceInEditMode;
    }

    public void setIsSourceInEditMode(boolean isSourceInEditMode) {
        this.isSourceInEditMode = isSourceInEditMode;
    }

    public boolean getIncludeAllPreviousVersions() {
        return includeAllPreviousVersions;
    }

    public void setIncludeAllPreviousVersions(
            boolean includeAllPreviousVersions) {
        this.includeAllPreviousVersions = includeAllPreviousVersions;
    }

    public String getSourceDocWcUid() {
        return sourceDocWcUid;
    }

    public void setSourceDocWcUid(String sourceDocWcUid) {
        try {
            this.sourceDocWcUid = sourceDocWcUid;
            if (sourceDocWcUid != null) {

                Map<String, String> versionList = calculateVersionList(
                        sourceDocWcUid);
                setSourceDocVersionList(versionList);

                if (versionList != null && versionList.size() > 0) {
                    setSourceDocRealUid(
                            EloraUtil.getLastKeyFromMap(versionList));
                }
            } else {
                sourceDocRealUid = null;
                sourceDocVersionList.clear();
            }
        } catch (Exception e) {
            log.error("[setSourceDocWcUid] ["
                    + documentManager.getPrincipal().getName()
                    + "] Error setting source doc for OM Process |"
                    + getCurrentDocument().getId() + "|. Error: "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.setSourceDoc"));
        }

    }

    public Map<String, String> getSourceDocVersionList() {
        return sourceDocVersionList;
    }

    public void setSourceDocVersionList(
            Map<String, String> sourceDocVersionList) {
        this.sourceDocVersionList = sourceDocVersionList;
    }

    public String getSourceDocRealUid() {
        return sourceDocRealUid;
    }

    public void setSourceDocRealUid(String sourceDocRealUid) {
        this.sourceDocRealUid = sourceDocRealUid;
        includeAllPreviousVersions = false;
        if (sourceDocWcUid != null) {
            DocumentModel wcDoc = documentManager.getDocument(
                    new IdRef(sourceDocWcUid));
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
            if (baseDoc != null && baseDoc.getId().equals(sourceDocRealUid)) {
                includeAllPreviousVersions = true;
            }
        }
    }

    // Actions

    public void saveSourceDoc() {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            currentDocument.setPropertyValue(
                    OmMetadataConstants.OM_IMPACTED_DOC_LIST, null);
            currentDocument.setPropertyValue(
                    OmMetadataConstants.OM_PROCESSED_DOC_LIST, null);

            currentDocument.setPropertyValue(
                    OmMetadataConstants.OM_INCLUDE_PREVIOUS_VERSIONS,
                    includeAllPreviousVersions);

            currentDocument.setPropertyValue(
                    OmMetadataConstants.OM_SOURCE_DOC_WC_UID, sourceDocWcUid);
            currentDocument.setPropertyValue(
                    OmMetadataConstants.OM_SOURCE_DOC_REAL_UID,
                    sourceDocRealUid);

            documentManager.saveDocument(currentDocument);

            isSourceInEditMode = false;

            Events.instance().raiseEvent(OmEventNames.OM_IMPACT_LIST_UPDATED);
            Events.instance().raiseEvent(
                    OmEventNames.OM_PROCESSED_LIST_UPDATED);

            DocumentModel sourceDoc = documentManager.getDocument(
                    new IdRef(sourceDocRealUid));
            String reference = (String) sourceDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            String comment = reference + " " + sourceDoc.getVersionLabel();
            if (includeAllPreviousVersions) {
                comment += " ( + previous versions )";
            }
            EloraEventHelper.fireEvent(OmEventNames.OM_SOURCE_DOC_SAVED,
                    currentDocument, comment);

        } catch (Exception e) {
            log.error("[saveSourceDoc] ["
                    + documentManager.getPrincipal().getName()
                    + "] Error saving source doc for OM Process |"
                    + getCurrentDocument().getId() + "|. Error: "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.om.saveSourceDoc"));
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDoc) {

        if (newCurrentDoc.getPropertyValue(
                OmMetadataConstants.OM_SOURCE_DOC_REAL_UID) != null) {
            isSourceInEditMode = false;

            sourceDocRealUid = (String) newCurrentDoc.getPropertyValue(
                    OmMetadataConstants.OM_SOURCE_DOC_REAL_UID);
            sourceDocWcUid = (String) newCurrentDoc.getPropertyValue(
                    OmMetadataConstants.OM_SOURCE_DOC_WC_UID);
            includeAllPreviousVersions = (boolean) newCurrentDoc.getPropertyValue(
                    OmMetadataConstants.OM_INCLUDE_PREVIOUS_VERSIONS);

            try {
                sourceDocVersionList = calculateVersionList(sourceDocWcUid);
            } catch (EloraException e) {
                sourceDocVersionList.clear();
            }

        } else {
            isSourceInEditMode = true;
            sourceDocRealUid = null;
            sourceDocWcUid = null;
            sourceDocVersionList.clear();
            includeAllPreviousVersions = false;
        }
    }

    private Map<String, String> calculateVersionList(String wcUid)
            throws EloraException {
        boolean onlyReleasedVersion = false;
        List<DocumentModel> docVersionList = EloraDocumentHelper.calculateDocVersionList(
                wcUid, onlyReleasedVersion, documentManager);

        Map<String, String> versionList = EloraDocumentHelper.convertDocVersionListIntoMap(
                docVersionList);

        return versionList;
    }

}
