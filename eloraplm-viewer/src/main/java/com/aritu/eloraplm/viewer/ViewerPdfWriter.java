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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class ViewerPdfWriter {

    private static final float SMALL_FONT_SIZE = 7;

    private static final float MEDIUM_FONT_SIZE = 9;

    private static final float LARGE_FONT_SIZE = 11;

    private static final float LARGEST_FONT_SIZE = 12;

    private static final PDFont REGULAR_FONT = PDType1Font.HELVETICA;

    private static final PDFont BOLD_FONT = PDType1Font.HELVETICA_BOLD;

    private static final float TITLE_BLOCK_CURRENT_LIFECYCLE_STATE_POS_X = 75;

    private static final float TITLE_BLOCK_CURRENT_LIFECYCLE_STATE_POS_Y = 70;

    private static final float TITLE_BLOCK_TITLE_POS_X = 284;

    private static final float TITLE_BLOCK_TITLE_POS_Y = 71;

    private static final float TITLE_BLOCK_LAST_CONTRIBUTOR_POS_X = 330;

    private static final float TITLE_BLOCK_LAST_CONTRIBUTOR_POS_Y = 95;

    private static final float TITLE_BLOCK_MODIFIED_POS_X = 75;

    private static final float TITLE_BLOCK_MODIFIED_POS_Y = 95;

    private static final float TITLE_BLOCK_DRW_VERSION_LABEL_POS_X = 51;

    private static final float TITLE_BLOCK_DRW_VERSION_LABEL_POS_Y = 114;

    private static final float TITLE_BLOCK_REFERENCE_POS_X = 58;

    private static final float TITLE_BLOCK_REFERENCE_POS_Y = 32;

    private static final float TITLE_BLOCK_VERSION_LABEL_POS_X = 50;

    private static final float TITLE_BLOCK_VERSION_LABEL_POS_Y = 32;

    private static final float DOC_CURRENT_LIFECYCLE_STATE_POS_X = 225;

    private static final float DOC_CURRENT_LIFECYCLE_STATE_POS_Y = 10;

    private static final float DOC_TITLE_POS_X = 0;

    private static final float DOC_TITLE_POS_Y = 25;

    private static final float DOC_LAST_CONTRIBUTOR_POS_X = 120;

    private static final float DOC_LAST_CONTRIBUTOR_POS_Y = 10;

    private static final float DOC_MODIFIED_POS_X = 200;

    private static final float DOC_MODIFIED_POS_Y = 10;

    private static final float DOC_REFERENCE_POS_X = 70;

    private static final float DOC_REFERENCE_POS_Y = 10;

    private static final float DOC_VERSION_LABEL_POS_X = 75;

    private static final float DOC_VERSION_LABEL_POS_Y = 10;

    private static final float SPEC_CURRENT_LIFECYCLE_STATE_POS_X = 411;

    private static final float SPEC_CURRENT_LIFECYCLE_STATE_POS_Y = 89;

    private static final float SPEC_TITLE_POS_X = 225;

    private static final float SPEC_TITLE_POS_Y = 62;

    private static final float SPEC_LAST_CONTRIBUTOR_POS_X = 311;

    private static final float SPEC_LAST_CONTRIBUTOR_POS_Y = 89;

    private static final float SPEC_MODIFIED_POS_X = 133;

    private static final float SPEC_MODIFIED_POS_Y = 89;

    private static final float SPEC_REFERENCE_POS_X = 524;

    private static final float SPEC_REFERENCE_POS_Y = 89;

    private static final float SPEC_VERSION_LABEL_POS_X = 440;

    private static final float SPEC_VERSION_LABEL_POS_Y = 89;

    private static final String ALIGN_OPTION_LEFT = "alignLeft";

    private static final String ALIGN_OPTION_CENTER = "alignCenter";

    private static final String ALIGN_OPTION_RIGHT = "alignRight";

    private static final String LIFECYCLE_COORDS = "lifecycleCoords";

    private static final String TITLE_COORDS = "titleCoords";

    private static final String LAST_CONTRIBUTOR_COORDS = "lastContributorCoords";

    private static final String MODIFIED_COORDS = "modifiedCoords";

    private static final String DRW_VERSION_COORDS = "drwVersionCoords";

    private static final String REFERENCE_COORDS = "referenceCoords";

    private static final String VERSION_LABEL_COORDS = "versionLabelCoords";

    private static final float LINE_POS_START_X = 10;

    private static final float LINE_POS_FINISH_X = 10;

    private static final float LINE_POS_START_Y = 35;

    private static final float LINE_POS_FINISH_Y = 35;

    private static final String TITLE_LABEL = "Title: ";

    private static final String REFERENCE_LABEL = "Reference: ";

    private static final String STATE_LABEL = "State: ";

    private static final String LAST_CONTRIBUTOR_LABEL = "Last contributor: ";

    private static final String BLANK_LABEL = "";

    private DocumentModel doc;

    private String action;

    private String docType;

    private DocumentModel baseVersion;

    private PDPageContentStream contentStream;

    private float rightEdgeXPos = 0;

    private float topEdgeYPos = 0;

    private Map<String, TextSettings> textSettingsMap;

    private class TextSettings {

        float posX;

        float posY;

        PDFont fontType;

        float fontSize;

        PDFont lblFontType;

        float lblFontSize;

        String alignOption;

        private TextSettings() {
        }

        public float getPosX() {
            return posX;
        }

        public void setPosX(float posX) {
            this.posX = posX;
        }

        public float getPosY() {
            return posY;
        }

        public void setPosY(float posY) {
            this.posY = posY;
        }

        public PDFont getFontType() {
            return fontType;
        }

        public void setFontType(PDFont fontType) {
            this.fontType = fontType;
        }

        public float getFontSize() {
            return fontSize;
        }

        public void setFontSize(float fontSize) {
            this.fontSize = fontSize;
        }

        public PDFont getLabelFontType() {
            return lblFontType;
        }

        public void setLabelFontType(PDFont lblFontType) {
            this.lblFontType = lblFontType;
        }

        public float getLabelFontSize() {
            return lblFontSize;
        }

        public void setLabelFontSize(float lblFontSize) {
            this.lblFontSize = lblFontSize;
        }

        public String getAlignOption() {
            return alignOption;
        }

        public void setAlignOption(String alignOption) {
            this.alignOption = alignOption;
        }
    }

    private DocumentModel getBaseVersion() throws EloraException {
        if (baseVersion == null) {
            DocumentModel wcDoc = null;
            if (doc.isImmutable()) {
                wcDoc = doc.getCoreSession().getWorkingCopy(doc.getRef());
            } else {
                wcDoc = doc;
            }
            baseVersion = EloraDocumentHelper.getBaseVersion(wcDoc);
        }

        return baseVersion;
    }

    public ViewerPdfWriter(DocumentModel doc, String action) {
        this.doc = doc;
        this.action = action;

        textSettingsMap = new HashMap<String, TextSettings>();

    }

    private void initializeTitleBlockCoords() {

        TextSettings ts = new TextSettings();

        ts.setPosX(TITLE_BLOCK_CURRENT_LIFECYCLE_STATE_POS_X);
        ts.setPosY(TITLE_BLOCK_CURRENT_LIFECYCLE_STATE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGE_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_CENTER);
        textSettingsMap.put(LIFECYCLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_TITLE_POS_X);
        ts.setPosY(TITLE_BLOCK_TITLE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_CENTER);
        textSettingsMap.put(TITLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_LAST_CONTRIBUTOR_POS_X);
        ts.setPosY(TITLE_BLOCK_LAST_CONTRIBUTOR_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(LAST_CONTRIBUTOR_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_MODIFIED_POS_X);
        ts.setPosY(TITLE_BLOCK_MODIFIED_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_CENTER);
        textSettingsMap.put(MODIFIED_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_DRW_VERSION_LABEL_POS_X);
        ts.setPosY(TITLE_BLOCK_DRW_VERSION_LABEL_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(DRW_VERSION_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_REFERENCE_POS_X);
        ts.setPosY(TITLE_BLOCK_REFERENCE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGEST_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_RIGHT);
        textSettingsMap.put(REFERENCE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(TITLE_BLOCK_VERSION_LABEL_POS_X);
        ts.setPosY(TITLE_BLOCK_VERSION_LABEL_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(VERSION_LABEL_COORDS, ts);

    }

    private void initializeDocCoords() {

        TextSettings ts = new TextSettings();

        ts.setPosX(rightEdgeXPos - DOC_CURRENT_LIFECYCLE_STATE_POS_X);
        ts.setPosY(DOC_CURRENT_LIFECYCLE_STATE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGE_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(LIFECYCLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX((rightEdgeXPos / 2) - DOC_TITLE_POS_X);
        ts.setPosY(DOC_TITLE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_CENTER);
        textSettingsMap.put(TITLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(rightEdgeXPos - DOC_LAST_CONTRIBUTOR_POS_X);
        ts.setPosY(DOC_LAST_CONTRIBUTOR_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(SMALL_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_RIGHT);
        textSettingsMap.put(LAST_CONTRIBUTOR_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(rightEdgeXPos - DOC_MODIFIED_POS_X);
        ts.setPosY(DOC_MODIFIED_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(SMALL_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_RIGHT);
        textSettingsMap.put(MODIFIED_COORDS, ts);
        //
        // ts.setPosX(DOC_DRW_VERSION_LABEL_POS_X);
        // ts.setPosY(DOC_DRW_VERSION_LABEL_POS_Y);
        // ts.setFontType(REGULAR_FONT);
        // ts.setFontSize(MEDIUM_FONT_SIZE);
        // ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(DRW_VERSION_COORDS, null);

        ts = new TextSettings();
        ts.setPosX(DOC_REFERENCE_POS_X);
        ts.setPosY(DOC_REFERENCE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGEST_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_RIGHT);
        textSettingsMap.put(REFERENCE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(DOC_VERSION_LABEL_POS_X);
        ts.setPosY(DOC_VERSION_LABEL_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        ts.setLabelFontType(REGULAR_FONT);
        ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(VERSION_LABEL_COORDS, ts);
    }

    private void initializeSpecCoords() {
        TextSettings ts = new TextSettings();

        ts.setPosX(SPEC_CURRENT_LIFECYCLE_STATE_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_CURRENT_LIFECYCLE_STATE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGE_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(LIFECYCLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(SPEC_TITLE_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_TITLE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_CENTER);
        textSettingsMap.put(TITLE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(SPEC_LAST_CONTRIBUTOR_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_LAST_CONTRIBUTOR_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(LAST_CONTRIBUTOR_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(SPEC_MODIFIED_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_MODIFIED_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(SMALL_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(MODIFIED_COORDS, ts);
        //
        // ts.setPosX(DOC_DRW_VERSION_LABEL_POS_X);
        // ts.setPosY(DOC_DRW_VERSION_LABEL_POS_Y);
        // ts.setFontType(REGULAR_FONT);
        // ts.setFontSize(MEDIUM_FONT_SIZE);
        // ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(DRW_VERSION_COORDS, null);

        ts = new TextSettings();
        ts.setPosX(SPEC_REFERENCE_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_REFERENCE_POS_Y);
        ts.setFontType(BOLD_FONT);
        ts.setFontSize(LARGE_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(REFERENCE_COORDS, ts);

        ts = new TextSettings();
        ts.setPosX(SPEC_VERSION_LABEL_POS_X);
        ts.setPosY(topEdgeYPos - SPEC_VERSION_LABEL_POS_Y);
        ts.setFontType(REGULAR_FONT);
        ts.setFontSize(MEDIUM_FONT_SIZE);
        // ts.setLabelFontType(REGULAR_FONT);
        // ts.setLabelFontSize(SMALL_FONT_SIZE);
        ts.setAlignOption(ALIGN_OPTION_LEFT);
        textSettingsMap.put(VERSION_LABEL_COORDS, ts);
    }

    public File editSinglePdf(Blob pdfBlob)
            throws IOException, COSVisitorException, EloraException {

        // We only edit the first page
        PDDocument pdfDoc = PDDocument.load(pdfBlob.getFile());
        PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
        PDPage page = (PDPage) catalog.getAllPages().get(0);

        rightEdgeXPos = page.getMediaBox().getWidth();
        topEdgeYPos = page.getMediaBox().getHeight();

        writePdfContent(pdfDoc, page);

        File file = File.createTempFile(pdfBlob.getFilename(), "_temp.pdf");
        pdfDoc.save(file);
        pdfDoc.close();

        return file;
    }

    public File editPdf(Blob pdfBlob, String docType)
            throws IOException, COSVisitorException, EloraException {

        PDDocument pdfDoc = PDDocument.load(pdfBlob.getFile());
        PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
        this.docType = docType;

        for (Object pageObj : catalog.getAllPages()) {
            PDPage page = (PDPage) pageObj;
            rightEdgeXPos = page.getMediaBox().getWidth();
            topEdgeYPos = page.getMediaBox().getHeight();
            if (docType.equals(EloraDoctypeConstants.CAD_DRAWING)) {
                initializeTitleBlockCoords();
            } else if (docType.equals(
                    EloraDoctypeConstants.BOM_SPECIFICATION)) {
                initializeSpecCoords();
            } else {
                initializeDocCoords();
            }
            writePdfContent(pdfDoc, page);
        }

        File file = File.createTempFile(pdfBlob.getFilename(), "_temp.pdf");
        pdfDoc.save(file);
        pdfDoc.close();

        return file;
    }

    private void writePdfContent(PDDocument pdfDoc, PDPage page)
            throws IOException, EloraException {
        contentStream = new PDPageContentStream(pdfDoc, page, true, true);

        if (docType.equals(EloraDoctypeConstants.CAD_DRAWING)) {
            writeDrawingContent();
        } else if (docType.equals(EloraDoctypeConstants.BOM_SPECIFICATION)) {
            writeSpecContent();
        } else {
            writeDocContent();
        }
        contentStream.close();
    }

    private void writeDrawingContent() throws IOException, EloraException {
        writeCurrentLifeCycleState(BLANK_LABEL);
        writeTitle(BLANK_LABEL);
        writeLastContributor(BLANK_LABEL);
        writeModified(BLANK_LABEL);

        if (doc.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
            writeDrawingVersionLabel();
        } else {
            if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                writeReference(BLANK_LABEL);
                writeVersionLabel(BLANK_LABEL);
            }
        }
    }

    private void writeSpecContent() throws IOException, EloraException {
        writeTitle(BLANK_LABEL);

        writeCurrentLifeCycleState(BLANK_LABEL);

        writeLastContributor(BLANK_LABEL);
        writeModified(BLANK_LABEL);

        writeReference(BLANK_LABEL);
        writeVersionLabel(BLANK_LABEL);
    }

    private void writeDocContent() throws IOException, EloraException {

        contentStream.drawLine(LINE_POS_START_X, LINE_POS_START_Y,
                rightEdgeXPos - LINE_POS_FINISH_X, LINE_POS_FINISH_Y);

        writeTitle(TITLE_LABEL);

        writeCurrentLifeCycleState(STATE_LABEL);

        writeLastContributor(LAST_CONTRIBUTOR_LABEL);
        writeModified(BLANK_LABEL);

        writeReference(REFERENCE_LABEL);
        writeVersionLabel(BLANK_LABEL);
    }

    private void writeCurrentLifeCycleState(String label)
            throws IOException, EloraException {
        String lifeCycleState;
        if (action.equals(ViewerActionConstants.ACTION_OVERWRITE)) {
            lifeCycleState = getBaseVersion().getCurrentLifeCycleState();
        } else {
            lifeCycleState = doc.getCurrentLifeCycleState();
        }

        writeText(contentStream, textSettingsMap.get(LIFECYCLE_COORDS),
                lifeCycleState, label);
    }

    private void writeTitle(String label) throws IOException {
        String title = doc.getTitle();
        writeText(contentStream, textSettingsMap.get(TITLE_COORDS), title,
                label);
    }

    private void writeLastContributor(String label)
            throws IOException, PropertyException, EloraException {
        String lastContributorLogin = null;
        if (action.equals(ViewerActionConstants.ACTION_OVERWRITE)) {
            lastContributorLogin = (String) getBaseVersion().getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
        } else {
            lastContributorLogin = (String) doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
        }

        UserNameResolverHelper unr = new UserNameResolverHelper();
        String lastContributor = unr.getUserFullName(lastContributorLogin);

        writeText(contentStream, textSettingsMap.get(LAST_CONTRIBUTOR_COORDS),
                lastContributor, label);
    }

    private void writeModified(String label)
            throws IOException, PropertyException, EloraException {
        String modified = "";
        GregorianCalendar cal = null;
        if (action.equals(ViewerActionConstants.ACTION_OVERWRITE)) {
            cal = (GregorianCalendar) getBaseVersion().getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_MODIFIED);
        } else {
            // We use the current time, as the document is going to save after,
            // and this will change the Modified property.
            // If we used the stored property, it would not be correct.
            cal = (GregorianCalendar) Calendar.getInstance();
        }
        if (cal != null) {
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            fmt.setCalendar(cal);
            modified = fmt.format(cal.getTime()) + " UTC";
        }

        writeText(contentStream, textSettingsMap.get(MODIFIED_COORDS), modified,
                label);
    }

    private void writeDrawingVersionLabel() throws IOException, EloraException {
        String versionLabel = getVersionLabel();
        writeText(contentStream, textSettingsMap.get(DRW_VERSION_COORDS),
                versionLabel, BLANK_LABEL);
    }

    private void writeReference(String label) throws IOException {
        String reference = (String) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        writeText(contentStream, textSettingsMap.get(REFERENCE_COORDS),
                reference, label);
    }

    private void writeVersionLabel(String label)
            throws IOException, EloraException {
        String versionLabel = getVersionLabel();
        writeText(contentStream, textSettingsMap.get(VERSION_LABEL_COORDS),
                versionLabel, label);
    }

    /**
     * @return
     * @throws EloraException
     */
    private String getVersionLabel() throws EloraException {
        String versionLabel = null;
        if (action.equals(ViewerActionConstants.ACTION_PROMOTE)) {
            versionLabel = doc.getVersionLabel();
        } else {
            versionLabel = getBaseVersion().getVersionLabel();
        }
        return versionLabel;
    }

    private void writeText(PDPageContentStream contentStream, TextSettings ts,
            String text, String label) throws IOException {
        if (ts != null) {
            contentStream.beginText();

            contentStream.setFont(ts.getFontType(), ts.getFontSize());
            contentStream.setNonStrokingColor(0, 0, 0);

            float realPosX = getRealPosition(contentStream, ts, label + text);

            contentStream.moveTextPositionByAmount(realPosX, ts.getPosY());

            if (label.length() > 0) {
                drawLabel(contentStream, ts, label);
            }

            drawText(contentStream, ts, text);

            contentStream.endText();
        }
    }

    private void drawText(PDPageContentStream contentStream, TextSettings ts,
            String text) throws IOException {
        contentStream.setFont(ts.getFontType(), ts.getFontSize());
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.drawString(text);
    }

    private void drawLabel(PDPageContentStream contentStream, TextSettings ts,
            String label) throws IOException {
        contentStream.setFont(ts.getLabelFontType(), ts.getLabelFontSize());
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.drawString(label);
    }

    private float getRealPosition(PDPageContentStream contentStream,
            TextSettings ts, String text) throws IOException {

        float textWidth = 0;
        switch (ts.getAlignOption()) {
        case ALIGN_OPTION_LEFT:
            return rightEdgeXPos - ts.getPosX();
        case ALIGN_OPTION_RIGHT:
            textWidth = (ts.getFontType().getStringWidth(text) / 1000.0f)
                    * ts.getFontSize();
            return rightEdgeXPos - ts.getPosX() - textWidth;

        case ALIGN_OPTION_CENTER:
            textWidth = (ts.getFontType().getStringWidth(text) / 1000.0f)
                    * ts.getFontSize();
            return rightEdgeXPos - ts.getPosX() - (textWidth / 2);
        }
        return 0;
    }

}
