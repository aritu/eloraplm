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
package com.aritu.eloraplm.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ViewerPdfUpdater {

    public static Blob createViewer(DocumentModel doc)
            throws COSVisitorException, IOException {
        List<Blob> viewerFiles = new ArrayList<>();

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            viewerFiles = EloraDocumentHelper.getBomRelatedCadFiles(doc);
        } else if (doc.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
            viewerFiles.add(EloraDocumentHelper.getDocumentViewerFile(doc));
        } else {
            return null;
        }
        if (viewerFiles.size() > 0) {
            return editViewer(doc, viewerFiles);
        } else {
            return null;
        }

    }

    public static Blob editViewer(DocumentModel doc, List<Blob> pdfList)
            throws IOException, COSVisitorException {
        // Edit just the first one
        PDDocument pdfDoc = PDDocument.load(pdfList.get(0).getFile());
        PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
        PDPage page = (PDPage) catalog.getAllPages().get(0);

        PDFont boldFont = PDType1Font.HELVETICA_BOLD;
        PDFont normalFont = PDType1Font.HELVETICA;
        float lblFontSize = 10;
        float txtFontSize = 12;

        PDPageContentStream contentStream = new PDPageContentStream(pdfDoc,
                page, true, true);

        String gap = "    ";

        float posX = 80;
        float posY = 830;
        String reference = (String) doc.getPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE);
        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            // Setup first row data
            String referenceLbl = "Reference: ";
            String versionLbl = "Ver: ";
            String version = doc.getVersionLabel();
            String titleLbl = "Title: ";
            String title = doc.getTitle();

            // Write first row
            contentStream.beginText();
            contentStream.moveTextPositionByAmount(posX, posY);
            writeText(referenceLbl, reference, gap, contentStream, boldFont,
                    normalFont, lblFontSize, txtFontSize);
            writeText(versionLbl, version, gap, contentStream, boldFont,
                    normalFont, lblFontSize, txtFontSize);
            writeText(titleLbl, title, gap, contentStream, boldFont,
                    normalFont, lblFontSize, txtFontSize);
            contentStream.endText();
        }

        // Setup second row data
        String stateLbl = "State: ";
        String state = doc.getCurrentLifeCycleState().toUpperCase();
        String lastContributorLbl = "Last contributor: ";
        String lastContributor = (String) doc.getPropertyValue(NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
        String lastModifiedLbl = "";
        String lastModified = null;

        // GregorianCalendar cal = (GregorianCalendar)
        // doc.getPropertyValue(NuxeoMetadataConstants.NX_DC_MODIFIED);

        // TODO: Utilizar el time actual para pintar en el pdf porque luego se
        // va a guardar el documento y cambiará automáticamente el last
        // modified. Si no se hiciera esto no coincidirían.
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        if (cal != null) {
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            fmt.setCalendar(cal);
            lastModified = fmt.format(cal.getTime()) + " UTC";
        }

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            posY = 816;
        } else {
            String text = stateLbl + state + gap + lastContributorLbl
                    + lastContributor + gap + lastModifiedLbl + lastModified;
            posX = getCenteredTextPosition(page, text, normalFont, txtFontSize);
            posY = 10;
        }

        // Write second row
        contentStream.beginText();
        contentStream.moveTextPositionByAmount(posX, posY);
        writeText(stateLbl, state, gap, contentStream, boldFont, normalFont,
                lblFontSize, txtFontSize);
        writeText(lastContributorLbl, lastContributor, gap, contentStream,
                boldFont, normalFont, lblFontSize, txtFontSize);
        writeText(lastModifiedLbl, lastModified, gap, contentStream, boldFont,
                normalFont, lblFontSize, txtFontSize);
        contentStream.endText();

        contentStream.close();

        File firstPageFile = File.createTempFile(reference, "_temp.pdf");
        pdfDoc.save(firstPageFile);
        pdfDoc.close();

        File finalFile = null;
        String fileName = null;
        if (pdfList.size() > 1) {
            finalFile = mergePDFs(firstPageFile, pdfList);
            fileName = reference + ".pdf";
        } else {
            finalFile = firstPageFile;
            fileName = pdfList.get(0).getFilename();
        }

        Blob fb = Blobs.createBlob(finalFile);
        fb.setMimeType("application/pdf");
        Framework.trackFile(finalFile, fb);
        fb.setFilename(fileName);

        return fb;
    }

    private static void writeText(String label, String value, String gap,
            PDPageContentStream contentStream, PDFont bold, PDFont normal,
            float lblFontSize, float txtFontSize) throws IOException {
        contentStream.setFont(bold, lblFontSize);
        contentStream.drawString(label);
        contentStream.setFont(normal, txtFontSize);
        contentStream.drawString(value);
        contentStream.drawString(gap);
    }

    private static File mergePDFs(File firstFile, List<Blob> fileList)
            throws IOException, COSVisitorException {
        PDFMergerUtility ut = new PDFMergerUtility();
        ut.addSource(new FileInputStream(firstFile));
        for (Blob blob : fileList.subList(1, fileList.size())) {
            // checkPdf(blob);
            ut.addSource(new FileInputStream(blob.getFile()));
        }
        return appendPDFs(ut);
    }

    private static File appendPDFs(PDFMergerUtility ut) throws IOException,
            COSVisitorException {
        File tempFile = File.createTempFile("finalFile", ".pdf");
        ut.setDestinationFileName(tempFile.getAbsolutePath());
        ut.mergeDocuments();
        return tempFile;
    }

    public Blob attachPdf(File source, File attachment) throws IOException,
            COSVisitorException {

        try {
            final PDDocument document = PDDocument.load(source);

            final PDEmbeddedFile embeddedFile = new PDEmbeddedFile(document,
                    new FileInputStream(attachment));

            embeddedFile.setSubtype("application/pdf");
            embeddedFile.setSize(10993);

            final PDComplexFileSpecification fileSpecification = new PDComplexFileSpecification();
            fileSpecification.setFile("artificial text.pdf");
            fileSpecification.setEmbeddedFile(embeddedFile);

            final Map<String, PDComplexFileSpecification> embeddedFileMap = new HashMap<String, PDComplexFileSpecification>();
            embeddedFileMap.put("artificial text.pdf", fileSpecification);

            final PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
            efTree.setNames(embeddedFileMap);

            final PDDocumentNameDictionary names = new PDDocumentNameDictionary(
                    document.getDocumentCatalog());
            names.setEmbeddedFiles(efTree);
            document.getDocumentCatalog().setNames(names);

            File tempFile = File.createTempFile("mergeTest", ".pdf");
            document.save(tempFile);
            Blob fb = Blobs.createBlob(tempFile);
            fb.setMimeType("application/pdf");
            // Framework.trackFile(tempFile, fb);
            fb.setFilename("mergeTest.pdf");
            document.close();

            return fb;
        } catch (Exception e) {
            return null;
        }
        // Files.write(Paths.get("attachment.pdf"), baos.toByteArray());
    }

    private static float getCenteredTextPosition(PDPage page, String text,
            PDFont font, float fontSize) throws IOException {
        float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
        // float titleHeight =
        // font.getFontDescriptor().getFontBoundingBox().getHeight()
        // / 1000 * fontSize;

        // TODO: cambiar para poder tener opcion de ajustar en altura. Tener en
        // cuenta cuando esta apaisado

        float x = (page.getMediaBox().getWidth() - titleWidth) / 2;
        // float y = page.getMediaBox().getHeight() - marginTop - titleheight

        return x;
    }

    // protected void checkPdf(Blob blob) {
    // if (!"application/pdf".equals(blob.getMimeType())) {
    // ("Blob " + blob.getFilename() + " is not a PDF.");
    // }
    // }

}
