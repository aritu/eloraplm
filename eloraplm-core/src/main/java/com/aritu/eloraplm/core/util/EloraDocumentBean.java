package com.aritu.eloraplm.core.util;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("eloraDocument")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class EloraDocumentBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    private transient EloraVersionLabelService eloraVersionLabelService;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public String getMajorToDisplay(DocumentModel docModel) {
        return eloraVersionLabelService.getMajor(docModel);
    }

    public boolean isZeroVersion(String versionLabel) {
        return eloraVersionLabelService.getZeroVersion().equals(versionLabel);
    }

    public String getVersionStatus(DocumentModel currentDoc,
            DocumentModel wcDoc) {
        try {
            return EloraDocumentHelper.getVersionStatus(currentDoc, wcDoc);
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.getVersionStatus"));
            return null;
        }
    }

    public DocumentModel getWorkingCopy(DocumentModel doc) {
        if (doc != null) {
            return documentManager.getWorkingCopy(doc.getRef());
        }
        return null;
    }

    public DocumentModel getDocumentFromUid(String uid) {
        if (uid != null && !uid.isEmpty()) {
            return documentManager.getDocument(new IdRef(uid));
        } else {
            return null;
        }
    }
}
