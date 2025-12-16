package com.aritu.eloraplm.core.util;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("eloraDocument")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class EloraDocumentBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EloraDocumentBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    private transient VersionLabelService versionLabelService;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public String getMajorToDisplay(DocumentModel docModel) {
        return versionLabelService.getMajor(docModel);
    }

    public boolean isZeroVersion(DocumentModel doc) {
        String versionLabel = doc.getVersionLabel();
        // It can be a zero version with a different label too
        return versionLabelService.getZeroVersion().equals(versionLabel)
                || documentManager.getVersions(doc.getRef()).isEmpty();
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
            try {
                return documentManager.getWorkingCopy(doc.getRef());
            } catch (NuxeoException e) {
                log.error("Error getting document working copy. Document id = |"
                        + doc.getId() + "|" + e.getMessage());
            }
        }
        return null;
    }

    public boolean existsUid(String uid) {
        if (uid != null && !uid.isEmpty()) {
            return documentManager.exists(new IdRef(uid));
        }
        return false;
    }

    public DocumentModel getDocumentFromUid(String uid) {
        if (uid != null && !uid.isEmpty()) {
            return documentManager.getDocument(new IdRef(uid));
        }
        return null;
    }

    public boolean isDocumentStateStatusReleasedOrObsolete(DocumentModel doc) {
        boolean result = false;
        String status = LifecyclesConfig.getStateStatus(
                doc.getCurrentLifeCycleState());
        if (EloraLifeCycleConstants.STATUS_RELEASED.equals(status)
                || EloraLifeCycleConstants.STATUS_OBSOLETE.equals(status)) {
            result = true;
        }
        return result;
    }

    public DocumentModel getNonProxyDocument(DocumentModel doc) {
        if (doc != null) {
            if (doc.isProxy()) {
                return documentManager.getSourceDocument(doc.getRef());
            } else {
                return doc;
            }
        }
        return null;
    }

    public String getDocumentLastComment(DocumentModel doc) {
        String lastComment = "";
        String logInitMsg = "[getDocumentLastComment] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            if (doc != null) {
                CommentableDocument commentableDoc = doc.getAdapter(
                        CommentableDocument.class);
                if (commentableDoc != null) {
                    List<DocumentModel> comments = commentableDoc.getComments();

                    if (comments != null && comments.size() > 0) {

                        DocumentModel commentDoc = comments.get(
                                comments.size() - 1);

                        if (commentDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_COMMENT_TEXT) != null) {
                            lastComment = (String) commentDoc.getPropertyValue(
                                    NuxeoMetadataConstants.NX_COMMENT_TEXT);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return lastComment;
    }

    public List<DocumentModel> getDocumentsInWsContentFilteredByNature(
            DocumentModel doc, String nature) {

        DocumentModelList documents = null;
        String logInitMsg = "[getDocumentsInWsContentFilteredByNature] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            if (doc != null) {
                String contentDocumentListsQuery = EloraQueryFactory.getOtherDocumentProxiesInsideAncestorFilteredByNatureQuery(
                        doc.getId(), nature);
                documents = documentManager.query(contentDocumentListsQuery);
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return documents;
    }

}
