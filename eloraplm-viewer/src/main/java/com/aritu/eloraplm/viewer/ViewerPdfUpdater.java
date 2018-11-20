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
package com.aritu.eloraplm.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.OverwriteOriginalViewerException;
import com.aritu.eloraplm.templating.adapters.FakeTemplateBasedDocumentAdapter;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ViewerPdfUpdater {

    private static final Log log = LogFactory.getLog(ViewerPdfUpdater.class);

    public static Blob createViewer(DocumentModel doc, String action) {
        String logInitMsg = "[createViewer] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";
        Blob viewerBlob = null;
        if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            try {
                log.trace(logInitMsg + "Creating viewer with action |" + action
                        + "| for doc |" + doc.getId() + "|");

                if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                    try {
                        viewerBlob = createViewerForItem(doc, action);
                        updateViewerProperty(doc, viewerBlob);
                    } catch (OverwriteOriginalViewerException e) {
                        log.trace(logInitMsg
                                + "Document viewer won't be updated because it's date is before or equal than implantation date");
                    }
                } else if (doc.getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING)) {
                    viewerBlob = createViewerForDrawing(doc, action);
                    updateViewerProperty(doc, viewerBlob);
                }
            } catch (Exception e) {
                // Write to the log, empty viewer property and show message
                updateViewerProperty(doc, null);

                log.error(logInitMsg + e.getMessage(), e);
            }
        }
        return viewerBlob;
    }

    private static Blob createViewerForItem(DocumentModel doc, String action)
            throws IOException, COSVisitorException, EloraException,
            OverwriteOriginalViewerException {
        String logInitMsg = "[createViewerForItem] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        Blob fb = null;
        File finalFile = null;
        File relatedViewerFile = null;
        String fileName = null;

        ViewerPdfWriter writer = new ViewerPdfWriter(doc, action);
        relatedViewerFile = createRelatedDocsViewer(doc, logInitMsg, finalFile,
                writer, action);
        finalFile = addContentFileToViewer(doc, logInitMsg, relatedViewerFile,
                writer);
        if (finalFile != null) {
            finalFile = addHistoryAndEbom(doc, finalFile);
            fileName = getFileName(doc, action);
            fb = Blobs.createBlob(finalFile);
            fb.setMimeType("application/pdf");
            Framework.trackFile(finalFile, fb);
            fb.setFilename(fileName);
        }
        return fb;
    }

    private static File addContentFileToViewer(DocumentModel doc,
            String logInitMsg, File relatedViewerFile, ViewerPdfWriter writer)
            throws IOException, COSVisitorException {
        File finalFile = null;
        try {
            DocumentBlobHolder bh = new DocumentBlobHolder(doc,
                    NuxeoMetadataConstants.NX_FILE_CONTENT);
            Blob contentBlob = null;
            if (bh.getBlob() != null) {
                contentBlob = getPdfFromBlob(bh, bh.getBlob());
            }
            if (contentBlob != null) {
                finalFile = writer.editPdf(contentBlob, doc.getType());
                if (finalFile != null) {
                    if (relatedViewerFile != null) {
                        finalFile = mergePDFs(finalFile, relatedViewerFile);
                    }
                }
            } else if (relatedViewerFile != null) {
                finalFile = relatedViewerFile;
            }
        } catch (EloraException e) {
            log.trace(logInitMsg
                    + "No content file or cannot convert to pdf for doc |"
                    + doc.getId() + "|. No need to add it to viewer");
        }
        return finalFile;
    }

    private static File createRelatedDocsViewer(DocumentModel doc,
            String logInitMsg, File finalFile, ViewerPdfWriter writer,
            String action) throws OverwriteOriginalViewerException {
        Map<Integer, DocumentModel> relatedDocs = getItemRelatedDocsForViewer(
                doc);
        for (DocumentModel relatedDoc : relatedDocs.values()) {
            try {
                File pdfFile = null;
                Blob blob = null;
                if (relatedDoc.getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING)) {
                    if (action.equals(ViewerActionConstants.ACTION_OVERWRITE)) {
                        checkImportationDateForOverwriteViewer(relatedDoc);
                    }
                    blob = getBaseFileBlob(relatedDoc);
                } else {
                    blob = getViewerFileOrFileContentBlob(relatedDoc);
                }
                if (blob != null) {
                    if (!relatedDoc.hasFacet(
                            EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                        pdfFile = writer.editPdf(blob, relatedDoc.getType());
                    } else {
                        pdfFile = blob.getFile();
                    }
                    if (pdfFile != null) {
                        if (finalFile == null) {
                            finalFile = pdfFile;
                        } else {
                            finalFile = mergePDFs(finalFile, pdfFile);
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof OverwriteOriginalViewerException) {
                    throw new OverwriteOriginalViewerException(relatedDoc);
                }
                log.error(logInitMsg
                        + "Do nothing, continue with the other documents");
            }
        }
        return finalFile;
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

        if (!lastModifiedDate.after(importationDate)) {
            throw new OverwriteOriginalViewerException(doc);
        }
    }

    private static File addHistoryAndEbom(DocumentModel doc, File finalFile)
            throws COSVisitorException, IOException {

        String templateName = "CharacteristicsHistoryAndEbomTemplate";
        TemplateBasedDocument tmplBasedDoc = new FakeTemplateBasedDocumentAdapter(
                doc, templateName);
        Blob blob = tmplBasedDoc.renderWithTemplate(templateName);
        blob.setMimeType("application/vnd.oasis.opendocument.text");
        BlobHolder bh = new SimpleBlobHolder(blob);
        blob = convertToPdf(bh);

        if (blob != null) {
            File historyFile = blob.getFile();
            finalFile = mergePDFs(finalFile, historyFile);
        }

        return finalFile;
    }

    private static String getFileName(DocumentModel doc, String action)
            throws EloraException {
        String fileName;
        String reference = (String) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);

        String formatedVersionLabel = getVersionLabel(doc, action).replace(".",
                "");
        fileName = reference + "_" + formatedVersionLabel + ".pdf";
        return fileName;
    }

    private static Blob getViewerFileOrFileContentBlob(DocumentModel relatedDoc)
            throws EloraException {
        Blob blob;
        BlobHolder bh = null;
        if (relatedDoc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            bh = new DocumentBlobHolder(relatedDoc,
                    EloraMetadataConstants.ELORA_ELOVWR_FILE);
        } else {
            bh = new DocumentBlobHolder(relatedDoc,
                    NuxeoMetadataConstants.NX_FILE_CONTENT);
        }
        blob = checkBlobHolderAndGetPdf(bh);
        return blob;
    }

    private static Blob getBaseFileBlob(DocumentModel relatedDoc) {
        Blob blob;
        BlobHolder bh = new DocumentBlobHolder(relatedDoc,
                EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
        blob = bh.getBlob();
        return blob;
    }

    public static Map<Integer, DocumentModel> getItemRelatedDocsForViewer(
            DocumentModel doc) {

        Map<Integer, DocumentModel> relDocs = new TreeMap<Integer, DocumentModel>();
        List<Integer> sortArray = new ArrayList<Integer>();

        List<Resource> documentPredicates = new ArrayList<Resource>();
        Resource hasCadDocumentPredicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        documentPredicates.add(hasCadDocumentPredicate);

        Resource hasDocumentPredicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_DOCUMENT);
        documentPredicates.add(hasDocumentPredicate);

        Resource hasSpecificationPredicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_SPECIFICATION);
        documentPredicates.add(hasSpecificationPredicate);

        List<Statement> stmts = EloraRelationHelper.getStatements(doc,
                documentPredicates);
        int i = 0;
        for (Statement stmt : stmts) {
            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
            if (stmtInfo.getViewerOrdering() != null
                    && stmtInfo.getViewerOrdering() != 0) {
                DocumentModel relatedDoc = RelationHelper.getDocumentModel(
                        stmtInfo.getObject(), doc.getCoreSession());
                sortArray.add(stmtInfo.getViewerOrdering());
                relDocs.put(i, relatedDoc);
                i++;
            }
        }
        return sortDocs(relDocs, sortArray);
    }

    /*
     * Function to order relatedDocs by viewer ordering. User can set the same
     * viewer ordering for different documents and if we don't use this
     * function, documents won't be merged because documents with the same order
     * will be overwritten
     */
    private static Map<Integer, DocumentModel> sortDocs(
            Map<Integer, DocumentModel> relDocs, List<Integer> sortArray) {
        Map<Integer, DocumentModel> relatedDocs = new TreeMap<Integer, DocumentModel>();
        if (sortArray.size() > 0) {
            int maxVal = Collections.max(sortArray) + 1;
            for (int j = 0; j < sortArray.size(); j++) {
                int index = sortArray.indexOf(Collections.min(sortArray));
                relatedDocs.put(j, relDocs.get(index));
                sortArray.set(index, maxVal);
            }
        }
        return relatedDocs;
    }

    private static Blob createViewerForDrawing(DocumentModel doc, String action)
            throws IOException, EloraException, COSVisitorException {
        Blob fb = null;
        Blob baseBlob;

        baseBlob = checkBlobHolderAndGetPdf(getViewerBaseBlobHolder(doc));

        if (baseBlob != null) {
            String fileName = baseBlob.getFilename();
            ViewerPdfWriter writer = new ViewerPdfWriter(doc, action);
            File pdfFile = writer.editPdf(baseBlob,
                    EloraDoctypeConstants.CAD_DRAWING);

            if (pdfFile != null) {
                fb = Blobs.createBlob(pdfFile);
                fb.setMimeType("application/pdf");
                Framework.trackFile(pdfFile, fb);
                fb.setFilename(fileName);
            }
        }
        return fb;
    }

    private static void updateViewerProperty(DocumentModel doc,
            Blob viewerBlob) {
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

    private static Blob checkBlobHolderAndGetPdf(BlobHolder bh)
            throws EloraException {
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new EloraException("File for viewer is null.");
        }

        blob = getPdfFromBlob(bh, blob);

        return blob;
    }

    private static Blob getPdfFromBlob(BlobHolder bh, Blob blob)
            throws EloraException {
        if (!isBlobPdf(blob)) {
            blob = convertToPdf(bh);
            if (!isBlobPdf(blob)) {
                throw new EloraException(
                        "Could not convert file for viewer to PDF.");
            }
        }
        return blob;
    }

    private static boolean isBlobPdf(Blob blob) {
        return blob.getMimeType() != null
                && blob.getMimeType().equals("application/pdf");
    }

    private static Blob convertToPdf(BlobHolder bh) {

        Blob result = null;

        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder resultBh = cs.convertToMimeType("application/pdf", bh,
                new HashMap<String, Serializable>());

        if (resultBh != null) {
            result = resultBh.getBlob();
        }
        return result;
    }

    private static File mergePDFs(File f1, File f2)
            throws IOException, COSVisitorException {
        PDFMergerUtility ut = new PDFMergerUtility();
        ut.addSource(new FileInputStream(f1));
        ut.addSource(new FileInputStream(f2));

        return appendPDFs(ut);
    }

    private static File appendPDFs(PDFMergerUtility ut)
            throws IOException, COSVisitorException {
        File tempFile = File.createTempFile("finalFile", ".pdf");
        ut.setDestinationFileName(tempFile.getAbsolutePath());
        ut.mergeDocuments();
        return tempFile;
    }

    public static BlobHolder getViewerBaseBlobHolder(DocumentModel doc) {
        BlobHolder bh = new DocumentBlobHolder(doc,
                EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
        return bh;
    }

    public static void addViewerBlob(DocumentModel doc, Blob blob) {
        DocumentHelper.addBlob(
                doc.getProperty(EloraMetadataConstants.ELORA_ELOVWR_FILE),
                blob);
    }

    public static void removeViewerBlob(DocumentModel doc) {
        DocumentHelper.removeProperty(doc,
                EloraMetadataConstants.ELORA_ELOVWR_FILE);
    }

    private static String getVersionLabel(DocumentModel doc, String action)
            throws EloraException {
        String versionLabel = null;
        if (action.equals(ViewerActionConstants.ACTION_PROMOTE)) {
            versionLabel = doc.getVersionLabel();
        } else {
            DocumentModel wcDoc = doc.getCoreSession().getWorkingCopy(
                    doc.getRef());
            versionLabel = EloraDocumentHelper.getBaseVersion(
                    wcDoc).getVersionLabel();
        }
        return versionLabel;
    }

}
