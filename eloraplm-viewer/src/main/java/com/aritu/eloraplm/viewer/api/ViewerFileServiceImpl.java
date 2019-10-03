/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.OverwriteOriginalViewerException;
import com.aritu.eloraplm.templating.api.TemplatingService;
import com.aritu.eloraplm.viewer.util.PdfWriterHelper;

/**
 * @author aritu
 *
 */
public class ViewerFileServiceImpl extends DefaultComponent
        implements ViewerFileService {

    private static Log log = LogFactory.getLog(ViewerFileServiceImpl.class);

    private static final String XP_TYPES = "types";

    private static final String XP_VIEWER_FILES = "viewerFiles";

    private static final String XP_MODIFIERS = "modifiers";

    private static final String SECTION_FILE = "file";

    private static final String SECTION_TEMPLATE = "template";

    private static final String PDF_MIMETYPE = "application/pdf";

    private Map<String, String> types;

    private Map<String, ViewerFileDescriptor> viewerFiles;

    private Map<String, ModifierDescriptor> modifiers;

    TemplatingService templatingService;

    @Override
    public void activate(ComponentContext context) {
        types = new HashMap<String, String>();
        viewerFiles = new HashMap<String, ViewerFileDescriptor>();
        modifiers = new HashMap<String, ModifierDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        types = null;
        viewerFiles = null;
        modifiers = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_TYPES:
            TypesDescriptor td = (TypesDescriptor) contribution;
            if (td.types != null) {
                types = td.types;
            } else {
                throw new NuxeoException("Types is null");
            }
            break;
        case XP_VIEWER_FILES:
            ViewerFileDescriptor viewerFile = (ViewerFileDescriptor) contribution;
            if (viewerFile.id != null) {
                viewerFiles.put(viewerFile.id, viewerFile);
            } else {
                throw new NuxeoException("Viewer file sent without an id");
            }
            break;
        case XP_MODIFIERS:
            ModifierDescriptor modifier = (ModifierDescriptor) contribution;
            if (modifier.id != null) {
                modifiers.put(modifier.id, modifier);
            } else {
                throw new NuxeoException("Modifier sent without an id");
            }
            break;
        }
    }

    @Override
    public ViewerFileDescriptor getViewerFileForType(String type)
            throws EloraException {
        if (type == null || type.isEmpty()) {
            throw new EloraException("Provided type is null or empty.");
        }

        if (types != null && types.containsKey(type)) {
            String viewerFileId = types.get(type);
            if (viewerFiles != null && viewerFiles.containsKey(viewerFileId)) {
                return viewerFiles.get(viewerFileId);
            }
        }

        return null;
    }

    public ModifierDescriptor getModifier(String id) throws EloraException {
        if (id == null || id.isEmpty()) {
            throw new EloraException("Provided modifier id is null or empty.");
        }

        if (modifiers != null && modifiers.containsKey(id)) {
            return modifiers.get(id);
        }

        return null;
    }

    @Override
    public void createViewer(DocumentModel doc, String action)
            throws Exception {

        if (doc == null) {
            throw new EloraException("Document is null.");
        }

        if (action == null || action.isEmpty()) {
            throw new EloraException("Action is null or empty.");
        }

        String logInitMsg = "[createViewer] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Creating viewer with action |" + action
                + "| for doc |" + doc.getId() + "|");

        ViewerFileDescriptor vfd = getViewerFileForType(doc.getType());
        if (vfd == null) {
            log.info(logInitMsg + "No viewer file defined for type |"
                    + doc.getType() + "|");
            return;
        }

        try {
            Blob viewerBlob = createViewerFileWithDescriptor(doc, vfd, action);
            updateViewerProperty(doc, viewerBlob);
            log.trace(logInitMsg + "Viewer created.");
        } catch (OverwriteOriginalViewerException e) {
            log.trace(logInitMsg + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "An error occurred while creating the viewer file for doc |"
                            + doc.getId() + "|. Error message: "
                            + e.getMessage(),
                    e);
            // We remove the viewer, avoiding to use the previous version
            log.trace(logInitMsg + "Remove viewer");
            updateViewerProperty(doc, null);
            log.trace(logInitMsg + "Viewer removed");

            throw e;
        }

    }

    private Blob createViewerFileWithDescriptor(DocumentModel doc,
            ViewerFileDescriptor vfd, String action)
            throws OverwriteOriginalViewerException, COSVisitorException,
            EloraException, IOException, ParseException {
        if (vfd.sections != null && vfd.sections.length > 0) {
            SortedMap<Integer, Blob> blobMap = new TreeMap<Integer, Blob>();
            for (ViewerFileSectionDescriptor section : vfd.sections) {
                blobMap = processSection(doc, section, blobMap, action);
            }

            String fileName = getFileName(doc);

            File viewerFile = combineBlobsAndGetFile(blobMap);

            Blob blob = Blobs.createBlob(viewerFile);
            blob.setMimeType(PDF_MIMETYPE);
            Framework.trackFile(viewerFile, blob);
            blob.setFilename(fileName);

            return blob;
        }

        return null;
    }

    private String getFileName(DocumentModel doc) {
        String reference = (String) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        String formatedVersionLabel = getFormattedVersionLabel(doc);

        return reference + "_" + formatedVersionLabel + ".pdf";
    }

    private String getFormattedVersionLabel(DocumentModel doc) {
        String versionLabel = null;
        if (doc.isImmutable()) {
            versionLabel = doc.getVersionLabel();
        } else {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(doc);
            if (baseVersion != null) {
                versionLabel = baseVersion.getVersionLabel();
            } else {
                versionLabel = doc.getVersionLabel();
            }
        }

        return versionLabel.replace(".", "");
    }

    private SortedMap<Integer, Blob> processSection(DocumentModel doc,
            ViewerFileSectionDescriptor section,
            SortedMap<Integer, Blob> blobMap, String action)
            throws EloraException, COSVisitorException, IOException,
            ParseException, OverwriteOriginalViewerException {
        Blob blob = null;

        if (section.relations != null && section.relations.length > 0) {
            blob = processSectionRelations(doc, section, action);
        } else {
            blob = processSectionFile(doc, doc, section.type, section.xpath,
                    section.template, section.modifier, action, false);
        }

        if (blob != null) {
            blobMap.put(section.order, blob);
        }

        return blobMap;
    }

    private Blob processSectionRelations(DocumentModel doc,
            ViewerFileSectionDescriptor section, String action)
            throws COSVisitorException, IOException, EloraException,
            ParseException, OverwriteOriginalViewerException {
        Blob blob = null;
        SortedMap<Integer, Blob> blobMap = new TreeMap<Integer, Blob>();

        for (ViewerFileSectionRelationDescriptor relation : section.relations) {

            String xpath = relation.xpath != null ? relation.xpath
                    : section.xpath;
            String template = relation.template != null ? relation.template
                    : section.template;
            String modifier = relation.modifier != null ? relation.modifier
                    : section.modifier;

            for (Map.Entry<Integer, DocumentModel> entry : getRelatedDocuments(
                    doc, relation.predicate, relation.inverse).entrySet()) {

                if (relation.checkImportationDateForOverwriteViewer
                        && (action.equals(
                                ViewerConstants.ACTION_OVERWRITE)
                                || action.equals(
                                        ViewerConstants.ACTION_OVERWRITE_AV))) {
                    checkImportationDateForOverwriteViewer(entry.getValue());
                }

                Blob docBlob = processSectionFile(doc, entry.getValue(),
                        section.type, xpath, template, modifier, action, true);
                blobMap.put(entry.getKey(), docBlob);
            }
        }

        if (!blobMap.isEmpty())

        {
            File sectionFile = combineBlobsAndGetFile(blobMap);
            blob = Blobs.createBlob(sectionFile);
        }

        return blob;
    }

    private static void checkImportationDateForOverwriteViewer(
            DocumentModel doc)
            throws ParseException, OverwriteOriginalViewerException {
        Date importationDate = new SimpleDateFormat("yyyy-MM-dd").parse(
                EloraConfig.generalConfigMap.get(
                        EloraConfigConstants.KEY_IMPLANTATION_DATE));

        GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        Date lastModifiedDate = lastModifiedGc.getTime();

        Serializable overwritten = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_OVERWRITE_OVERWRITTEN);

        if (!lastModifiedDate.after(importationDate) && overwritten == null) {
            throw new OverwriteOriginalViewerException(doc);
        }
    }

    private Blob processSectionFile(DocumentModel metadataDoc,
            DocumentModel blobDoc, String type, String xpath, String template,
            String modifier, String action, boolean required)
            throws EloraException, COSVisitorException, IOException {
        Blob blob = null;

        switch (type) {
        case SECTION_FILE:
            BlobHolder bh = new DocumentBlobHolder(blobDoc, xpath);
            blob = bh.getBlob();
            if (required && blob == null) {
                throw new EloraException(
                        "Required file for viewer is null for doc |"
                                + blobDoc.getId() + "| in xpath |" + xpath
                                + "|.");
            }
            break;
        case SECTION_TEMPLATE:
            blob = getTemplatingService().processTemplate(template, blobDoc);
            blob = convertBlobToPdf(blob);
            break;
        }

        if (blob != null) {
            if (!blob.getMimeType().equals(PDF_MIMETYPE)) {
                blob = convertBlobToPdf(blob);
            }

            if (modifier != null) {
                blob = applyModifier(blob, modifier, metadataDoc, action);
            }
        }

        return blob;
    }

    private TemplatingService getTemplatingService() {
        if (templatingService == null) {
            templatingService = Framework.getService(TemplatingService.class);
        }
        return templatingService;
    }

    private Map<Integer, DocumentModel> getRelatedDocuments(DocumentModel doc,
            String predicateUri, boolean inverse) {
        Map<Integer, DocumentModel> map = new HashMap<Integer, DocumentModel>();

        Resource predicate = new ResourceImpl(predicateUri);
        List<Statement> stmts;
        if (inverse) {
            stmts = EloraRelationHelper.getSubjectStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, doc, predicate);
        } else {
            stmts = RelationHelper.getStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, doc, predicate);
        }
        for (Statement stmt : stmts) {
            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
            Integer order = inverse ? stmtInfo.getInverseViewerOrdering()
                    : stmtInfo.getViewerOrdering();
            if (order != null && order != 0) {
                Node relatedNode = inverse ? stmtInfo.getSubject()
                        : stmtInfo.getObject();
                DocumentModel relatedDoc = RelationHelper.getDocumentModel(
                        relatedNode, doc.getCoreSession());
                map.put(order, relatedDoc);
            }
        }

        return map;
    }

    private Blob convertBlobToPdf(Blob blob) {
        Blob result = null;

        BlobHolder bh = new SimpleBlobHolder(blob);
        ConversionService cs = Framework.getService(ConversionService.class);
        BlobHolder resultBh = cs.convertToMimeType(PDF_MIMETYPE, bh,
                new HashMap<String, Serializable>());

        if (resultBh != null) {
            result = resultBh.getBlob();
        }

        return result;
    }

    private Blob applyModifier(Blob blob, String modifierId, DocumentModel doc,
            String action)
            throws EloraException, COSVisitorException, IOException {
        ModifierDescriptor modifier = getModifier(modifierId);
        if (modifier == null) {
            throw new EloraException(
                    "There is no modifier defined with provided id |"
                            + modifierId + "|");
        }

        PdfWriterHelper pwh = new PdfWriterHelper(blob, modifier, doc, action);
        File file = pwh.writePdf();
        blob = Blobs.createBlob(file);

        return blob;
    }

    private File combineBlobsAndGetFile(SortedMap<Integer, Blob> blobMap)
            throws COSVisitorException, IOException {
        PDFMergerUtility ut = new PDFMergerUtility();
        for (Blob blob : blobMap.values()) {
            File f = blob.getFile();
            ut.addSource(new FileInputStream(f));
        }

        return mergeFiles(ut);
    }

    private File mergeFiles(PDFMergerUtility ut)
            throws IOException, COSVisitorException {
        File tempFile = File.createTempFile("tempFile", ".pdf");
        ut.setDestinationFileName(tempFile.getAbsolutePath());
        ut.mergeDocuments();
        return tempFile;
    }

    private void updateViewerProperty(DocumentModel doc, Blob viewerBlob) {
        String logInitMsg = "[updateViewerProperty] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (viewerBlob != null) {
            log.trace(logInitMsg + "Add viewer");
            addViewerBlob(doc, viewerBlob);
            log.trace(logInitMsg + "Viewer added");
        } else {
            log.trace(logInitMsg + "Remove viewer");
            removeViewerBlob(doc);
            log.trace(logInitMsg + "Viewer removed");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void addViewerBlob(DocumentModel doc, Blob blob) {
        DocumentHelper.addBlob(
                doc.getProperty(EloraMetadataConstants.ELORA_ELOVWR_FILE),
                blob);
    }

    private void removeViewerBlob(DocumentModel doc) {
        DocumentHelper.removeProperty(doc,
                EloraMetadataConstants.ELORA_ELOVWR_FILE);
    }
}
