/**
 *
 */
package com.aritu.eloraplm.securitypolicy.api;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraMimeTypeConstants;
import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.UnableToConvertBlobToPdfException;
import com.aritu.eloraplm.securitypolicy.constants.SecurityFacetConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityViewerEventNames;
import com.aritu.eloraplm.securitypolicy.util.SecurityInfo;
import com.aritu.eloraplm.viewer.api.ViewerFileService;
import com.aritu.eloraplm.viewer.util.PdfWriterHelper;

/**
 * @author aritu
 *
 */
public class SecurityConfigServiceImpl extends DefaultComponent
        implements SecurityConfigService {

    private static final Log log = LogFactory.getLog(
            SecurityConfigServiceImpl.class);

    private static final String XP_LEVELS = "levels";

    private static final String XP_TYPES = "types";

    private Map<Integer, LevelDescriptor> levels;

    private Map<String, TypeDescriptor> types;

    @Override
    public void activate(ComponentContext context) {
        levels = new HashMap<Integer, LevelDescriptor>();
        types = new HashMap<String, TypeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        levels = null;
        types = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_LEVELS:
            LevelDescriptor ld = (LevelDescriptor) contribution;
            if (ld != null) {
                if (ld.id == null) {
                    throw new NuxeoException("Level sent without id");
                }
                if (levels.containsKey(ld.id)) {
                    LevelDescriptor currentLd = levels.get(ld.id);
                    currentLd.merge(ld);
                    levels.put(currentLd.id, currentLd);
                } else {
                    levels.put(ld.id, ld);
                }
            }
            break;
        case XP_TYPES:
            TypeDescriptor td = (TypeDescriptor) contribution;
            if (td != null) {
                if (td.id == null) {
                    throw new NuxeoException("Type sent without id");
                }
                if (types.containsKey(td.id)) {
                    TypeDescriptor currentTd = types.get(td.id);
                    currentTd.merge(td);
                    types.put(currentTd.id, currentTd);
                } else {
                    types.put(td.id, td);
                }
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    // ------------------------
    // Levels
    // ------------------------

    @Override
    public Map<Integer, String> getLevelsMap() {
        Map<Integer, String> levelsMap = new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, LevelDescriptor> entry : levels.entrySet()) {
            String label = getLevelLabel(entry.getKey());
            levelsMap.put(entry.getKey(), label);
        }
        return levelsMap;
    }

    @Override
    public LevelDescriptor getLevelConfig(Integer id) {
        if (id != null && levels.containsKey(id)) {
            return levels.get(id);
        }
        return null;
    }

    @Override
    public String getLevelLabel(Integer id) {
        LevelDescriptor ld = getLevelConfig(id);
        if (ld != null && ld.label != null) {
            return ld.label;
        }
        return "";
    }

    @Override
    public String getLevelModifier(Integer id) {
        LevelDescriptor ld = getLevelConfig(id);
        if (ld != null && ld.modifier != null) {
            return ld.modifier;
        }
        return null;
    }

    @Override
    public String getLevelColor(Integer id) {
        LevelDescriptor ld = getLevelConfig(id);
        if (ld != null && ld.color != null) {
            return ld.color;
        }
        return "#000000";
    }

    // ------------------------
    // Types
    // ------------------------

    @Override
    public TypeDescriptor getTypeConfig(String id) {
        if (id != null && types.containsKey(id)) {
            return types.get(id);
        }
        return null;
    }

    @Override
    public Integer getTypeLevel(String id) {
        TypeDescriptor td = getTypeConfig(id);
        if (td != null && td.level != null) {
            return td.level;
        }
        return 1;
    }

    @Override
    public void createSecurityViewer(DocumentModel doc) throws Exception {

        if (doc == null) {
            throw new EloraException("Document is null.");
        }

        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[createSecurityViewer] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Creating security viewer for doc |"
                + doc.getId() + "|");

        try {
            doc = getCorrectDocument(doc, session, logInitMsg);
            if (doc != null) {
                addSecurityViewerFile(doc, logInitMsg);
            }
        } catch (Exception e) {
            log.error(
                    "An error occurred while creating the security viewer file for doc |"
                            + doc.getId() + "|. Error message: "
                            + e.getMessage(),
                    e);
            throw e;
        }

        log.trace(logInitMsg + "--- EXIT --- ");

    }

    private void addSecurityViewerFileToAllVersions(DocumentModel doc,
            SecurityInfo securityInfo, CoreSession session, String logInitMsg)
            throws EloraException, Exception {
        if (doc.isImmutable()) {
            doc = session.getWorkingCopy(doc.getRef());
        }

        List<DocumentRef> versionRefs = session.getVersionsRefs(doc.getRef());

        for (DocumentRef docRef : versionRefs) {
            DocumentModel avDoc = session.getDocument(docRef);
            avDoc = updateDocumentSecurityInfo(avDoc, securityInfo, session);
            addSecurityViewerFile(avDoc, logInitMsg);
        }
    }

    private DocumentModel updateDocumentSecurityInfo(DocumentModel doc,
            SecurityInfo securityInfo, CoreSession session)
            throws EloraException {

        EloraDocumentHelper.disableVersioningDocument(doc);

        updateSecurityInfoMetadata(doc, securityInfo);

        return session.saveDocument(doc);
    }

    private void updateSecurityInfoMetadata(DocumentModel doc,
            SecurityInfo securityInfo) {
        doc.setPropertyValue(SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL,
                securityInfo.getLevelId());
        doc.setPropertyValue(SecurityPolicyMetadataConstants.SEC_SECURITY_DATE,
                securityInfo.getLevelSetDate());
        doc.setPropertyValue(SecurityPolicyMetadataConstants.SEC_SECURITY_BY,
                securityInfo.getLevelSetBy());
    }

    private void addSecurityViewerFile(DocumentModel doc, String logInitMsg)
            throws Exception, EloraException {
        Blob blob = null;
        blob = (Blob) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELOVWR_FILE);

        if (blob == null) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| does not have viewer file and security viewer file is not created");
            return;
        }

        // TODO: Arreglo para lanzar operación masiva en Ecenarro. A futuro se
        // tiene que controlar dentro de viewerFileService y lanzar una
        // excepción personalizada.
        if (blob.getLength() == 0) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| has empty viewer file and security viewer file is not created");
            return;
        }

        Blob secViewerBlob = applyModifierToBlob(doc, blob, logInitMsg);

        if (secViewerBlob != null) {
            doc = addSecurityViewerBlob(doc, secViewerBlob,
                    doc.getCoreSession());
            log.trace(logInitMsg + "Security viewer added to document |"
                    + doc.getId());
        }
    }

    private DocumentModel getCorrectDocument(DocumentModel doc,
            CoreSession session, String logInitMsg) {
        if (doc.isProxy()) {
            doc = session.getSourceDocument(doc.getRef());
        }

        if (!doc.hasFacet(SecurityFacetConstants.FACET_HAS_SECURITY_INFO)) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| does not have security facet and security viewer file is not created");
            return null;
        }
        return doc;
    }

    private Blob applyModifierToBlob(DocumentModel doc, Blob blob,
            String logInitMsg) throws Exception {

        Long secLevelId = (Long) doc.getPropertyValue(
                SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL);
        String secLevelModifier = getLevelModifier(secLevelId.intValue());

        if (secLevelModifier == null) {
            doc = addSecurityViewerBlob(doc, blob, doc.getCoreSession());
            log.trace(logInitMsg
                    + "Security viewer created without modifier for doc "
                    + doc.getId());
            return null;
        }
        return createBlobWithModifier(doc, blob, secLevelModifier);
    }

    private Blob createBlobWithModifier(DocumentModel doc, Blob blob,
            String secLevelModifier) throws EloraException, IOException,
            COSVisitorException, UnableToConvertBlobToPdfException {
        ViewerFileService vfs = Framework.getService(ViewerFileService.class);

        if (!blob.getMimeType().equals(
                EloraMimeTypeConstants.APPLICATION_PDF)) {
            blob = vfs.convertBlobToPdf(blob);
        }

        PdfWriterHelper pwh = new PdfWriterHelper(blob,
                vfs.getModifier(secLevelModifier), doc,
                ViewerConstants.ACTION_PROMOTE);

        File signedFile = pwh.writePdf();

        Blob secViewerBlob = Blobs.createBlob(signedFile);
        secViewerBlob.setMimeType(EloraMimeTypeConstants.APPLICATION_PDF);
        Framework.trackFile(signedFile, secViewerBlob);
        secViewerBlob.setFilename(blob.getFilename());
        return secViewerBlob;
    }

    private DocumentModel addSecurityViewerBlob(DocumentModel doc, Blob blob,
            CoreSession session) throws EloraException {

        DocumentHelper.addBlob(doc.getProperty(
                SecurityPolicyMetadataConstants.SEC_SECURITY_VIEWER_FILE),
                blob);

        if (doc.isImmutable()) {
            // From promote/demote. After this, data is copied (restore) from AV
            // to WC and it's ok because AV has the correct security viewer file
            EloraDocumentHelper.disableVersioningDocument(doc);
        } else if (!doc.isCheckedOut()) {
            // After checkin action, data is copied (restore) from AV to WC so
            // we need to put security viewer file in AV. If not, WC security
            // viewer file is replaced by the old one in the AV
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            EloraDocumentHelper.disableVersioningDocument(baseDoc);
            DocumentHelper.addBlob(baseDoc.getProperty(
                    SecurityPolicyMetadataConstants.SEC_SECURITY_VIEWER_FILE),
                    blob);
            session.saveDocument(baseDoc);
        }
        // From overwrite the document arrives as wc checkedOut. After this
        // event data is copied from WC to AV and then WC is restored to AV. So
        // we don't have to do anything.

        return session.saveDocument(doc);
    }

    @Override
    public DocumentModel updateSecurityInfo(DocumentModel doc,
            SecurityInfo securityInfo) throws Exception {

        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[updateSecurityInfo] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Updating security info for doc |" + doc.getId()
                + "|");

        doc = getCorrectDocument(doc, session, logInitMsg);

        if (doc != null) {
            addSecurityViewerFileToAllVersions(doc, securityInfo, session,
                    logInitMsg);

            if (!doc.isCheckedOut()) {
                DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
                doc = EloraDocumentHelper.restoreToVersion(doc.getRef(),
                        baseDoc.getRef(), true, true, session);
            } else {
                doc = updateDocumentSecurityInfo(doc, securityInfo, session);
            }

            EloraEventHelper.fireEvent(
                    SecurityViewerEventNames.SECURITY_INFO_UPDATED_EVENT, doc,
                    String.valueOf(securityInfo.getLevelId()));

        }
        log.trace(logInitMsg + "--- EXIT --- ");

        return doc;
    }

    @Override
    public DocumentModel configureDefaultSecurityLevel(DocumentModel doc)
            throws EloraException {

        if (doc == null) {
            throw new EloraException("Document is null.");
        }

        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[setDefaultSecurityLevel] ["
                + session.getPrincipal().getName() + "] ";

        if (!doc.hasFacet(SecurityFacetConstants.FACET_HAS_SECURITY_INFO)) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| does not have security facet and default security level is not set");
            return null;
        }

        log.trace(logInitMsg + "Setting default security level for doc |"
                + doc.getId() + "|");

        List<DocumentRef> versionRefs = session.getVersionsRefs(doc.getRef());
        if (versionRefs.size() > 0) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| has archived versions and default security level is not going to be configured");
            return null;
        }

        try {
            Integer levelId = getTypeLevel(doc.getType());

            Principal principal = session.getPrincipal();
            if (principal instanceof EloraPrincipalImpl) {
                EloraPrincipal eloPrincipal = (EloraPrincipal) principal;
                Integer secLevelPrincipal = eloPrincipal.getSecurityLevel();
                if (levelId > secLevelPrincipal) {
                    // We set the top level of the user
                    levelId = secLevelPrincipal;
                }
            }

            SecurityInfo secInfo = new SecurityInfo(levelId, new Date(),
                    session.getPrincipal().getName());

            updateSecurityInfoMetadata(doc, secInfo);

            doc = session.saveDocument(doc);

        } catch (Exception e) {
            log.error(
                    "An error occurred while setting the security level for doc |"
                            + doc.getId() + "|. Error message: "
                            + e.getMessage(),
                    e);
            throw e;
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return doc;

    }

}
