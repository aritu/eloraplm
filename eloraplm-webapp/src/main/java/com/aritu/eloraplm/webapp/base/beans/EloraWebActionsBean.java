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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Name("eloraWebActions")
@Scope(CONVERSATION)
public class EloraWebActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EloraWebActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private Boolean isAny2PdfConverterAvailable;

    /**
     * @param directoryName
     * @param entryId
     * @return
     */
    public static DocumentModel getDirectoryEntry(String directoryName,
            String entryId) {
        if (entryId == null) {
            return null;
        }
        DirectoryService dirService = Framework.getService(
                DirectoryService.class);
        try (Session session = dirService.open(directoryName)) {
            return session.getEntry(entryId);
        }
    }

    /**
     * @return
     */
    public boolean isInAWorkspace() {

        DocumentModel superSpace = documentManager.getSuperSpace(
                navigationContext.getCurrentDocument());

        if (superSpace.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            return true;
        }

        return false;
    }

    public DocumentRef getDocumentRefFromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        DocumentRef docRef = new IdRef(id);

        return docRef;
    }

    public DocumentModel getDocument(DocumentRef docRef) {
        String logInitMsg = "[getDocument] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (docRef == null) {
            return null;
        }
        DocumentModel docM = null;
        try {

            docM = documentManager.getDocument(docRef);

        } catch (DocumentNotFoundException | DocumentSecurityException e) {
            log.error(logInitMsg + "Document with docRef = |" + docRef
                    + "| cannot be retrieved. It does not exist any more or the user has no permission to read it. Exception message = |"
                    + e.getMessage() + "|");

            return null;
        }

        return docM;
    }

    public boolean isAvObsolete() {
        try {
            return EloraDocumentHelper.isAvObsolete(
                    navigationContext.getCurrentDocument());
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.checkIfDocIsObsolete"));
            return false;
        }
    }

    public boolean isWcObsolete() {
        try {
            return EloraDocumentHelper.isWcObsolete(
                    navigationContext.getCurrentDocument());
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.checkIfDocIsObsolete"));
            return false;
        }
    }

    /**
     * @param doc
     * @return
     */
    public boolean isEditable() {
        return EloraDocumentHelper.isEditable(
                navigationContext.getCurrentDocument());
    }

    public boolean hasBaseVersion() {
        if (EloraDocumentHelper.getBaseVersion(
                navigationContext.getCurrentDocument()) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param expr
     * @return
     */
    public Boolean evaluateBoolean(String expr) {
        FacesContext context = FacesContext.getCurrentInstance();
        Boolean result = context.getApplication().evaluateExpressionGet(context,
                expr, Boolean.class);

        return result;
    }

    /**
     * @param expr
     * @return
     */
    public String evaluateString(String expr) {

        FacesContext context = FacesContext.getCurrentInstance();
        String result = context.getApplication().evaluateExpressionGet(context,
                expr, String.class);

        return result;
    }

    public Object evaluateObject(String expr) {

        FacesContext context = FacesContext.getCurrentInstance();
        Object result = context.getApplication().evaluateExpressionGet(context,
                expr, Object.class);

        return result;
    }

    // public List<Object> reverseListOrder(List<Object> list) {
    // Collections.reverse(list);
    // return list;
    // }

    public String getEloraDownloadLink() throws EloraException {

        String logInitMsg = "[getEloraDownloadLink] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }
        if (!currentDoc.isImmutable()) {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            if (baseDoc == null) {
                log.error(logInitMsg + "The document |" + currentDoc.getId()
                        + "| has no base version. Probably because it has no AVs.");
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.getEloraDownloadLink"));
            }
            currentDoc = baseDoc;
        }

        return "elora://" + documentManager.getPrincipal().getName() + "/"
                + currentDoc.getId() + "/"
                + currentDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL)
                + "/" + currentDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION);
    }

    public void registerDownloadAndOpenAction() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }
        if (!currentDoc.isImmutable()) {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            if (baseVersion != null) {
                currentDoc = baseVersion;
            }
        }

        // Nuxeo Event
        String comment = currentDoc.getVersionLabel();
        EloraEventHelper.fireEvent(
                EloraEventNames.ELORA_DOWNLOAD_AND_OPEN_EVENT, currentDoc,
                comment);
    }

    public Boolean getIsAny2PdfConverterAvailable() {
        if (isAny2PdfConverterAvailable == null) {
            isAny2PdfConverterAvailable = Boolean.valueOf(
                    checkIfAny2PdfConverterIsAvailable());
        }

        return isAny2PdfConverterAvailable;

    }

    private boolean checkIfAny2PdfConverterIsAvailable() {
        ConversionService cs = Framework.getService(ConversionService.class);
        try {
            cs.isConverterAvailable("any2pdf");
            return true;
        } catch (ConverterNotRegistered e) {
            return false;
        }

    }

    public Map<Object, Object> getEloraPlmProperties() {
        Properties props = Framework.getProperties();
        Map<Object, Object> eloraProps = new TreeMap<Object, Object>();
        props.forEach((key, value) -> {
            if (key.toString().startsWith("com.aritu.eloraplm.")) {
                eloraProps.put(key, value);
            }
        });

        return eloraProps;
    }
}
