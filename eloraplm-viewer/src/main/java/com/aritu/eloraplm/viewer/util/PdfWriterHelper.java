/**
 *
 */
package com.aritu.eloraplm.viewer.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.api.ModifierDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierImageDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierLineDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierRectDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierTextDescriptor;
import com.aritu.eloraplm.viewer.dataevaluator.api.DataObtainerAdapter;
import com.aritu.eloraplm.viewer.dataevaluator.util.ConditionEvaluatorHelper;
import com.aritu.eloraplm.viewer.dataevaluator.util.PostProcessingHelper;
import com.aritu.eloraplm.viewer.dataevaluator.util.ValueObtainerHelper;

/**
 * @author aritu
 *
 */
public class PdfWriterHelper {

    private static final String OPAQUE_GS_ID = "opaque";

    private static final String REF_POINT_X_OPTION_LEFT = "left";

    private static final String REF_POINT_X_OPTION_CENTER = "center";

    private static final String REF_POINT_X_OPTION_RIGHT = "right";

    private static final String REF_POINT_Y_OPTION_TOP = "top";

    private static final String REF_POINT_Y_OPTION_CENTER = "center";

    private static final String REF_POINT_Y_OPTION_BOTTOM = "bottom";

    private static final String ALIGN_OPTION_LEFT = "left";

    private static final String ALIGN_OPTION_RIGHT = "right";

    private static final String ALIGN_OPTION_CENTER = "center";

    private Blob blob;

    private ModifierDescriptor modifier;

    private DocumentModel currentDoc;

    private DocumentModel relatedDoc;

    private String action;

    public PdfWriterHelper(Blob blob, ModifierDescriptor modifier,
            DocumentModel currentDoc, String action) throws EloraException {
        this(blob, modifier, currentDoc, null, action);
    }

    public PdfWriterHelper(Blob blob, ModifierDescriptor modifier,
            DocumentModel currentDoc, DocumentModel relatedDoc, String action)
            throws EloraException {
        if (blob == null) {
            throw new EloraException("Provided blob is null.");
        }

        if (modifier == null) {
            throw new EloraException("Provided ModifierDescriptor is null.");
        }

        if (currentDoc == null) {
            throw new EloraException("Provided doc is null.");
        }

        if (action == null) {
            throw new EloraException("Provided action is null.");
        }

        this.blob = blob;
        this.modifier = modifier;
        this.currentDoc = currentDoc;
        this.relatedDoc = relatedDoc;
        this.action = action;
    }

    public File writePdf()
            throws EloraException, IOException, COSVisitorException {
        File file = null;
        PDDocument pdfDoc = PDDocument.load(blob.getFile());
        try {
            PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
            List<?> pageList = catalog.getAllPages();
            for (Object obj : pageList) {
                PDPage page = (PDPage) obj;
                writePage(pdfDoc, page);
            }

            file = File.createTempFile(blob.getFilename(), "_temp.pdf");
            pdfDoc.save(file);

        } finally {
            pdfDoc.close();
        }

        return file;
    }

    /**
     * Writes the PDF applying the modifier in the specified pageNumber, instead
     * of applying it in the whole blob.
     *
     * @param pageNumber
     * @return
     * @throws EloraException
     * @throws IOException
     * @throws COSVisitorException
     */
    public File writePdf(int pageNumber)
            throws EloraException, IOException, COSVisitorException {
        File file = null;
        PDDocument pdfDoc = PDDocument.load(blob.getFile());
        try {
            PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
            List<?> pageList = catalog.getAllPages();
            PDPage page = (PDPage) pageList.get(pageNumber - 1);
            writePage(pdfDoc, page);
            file = File.createTempFile(blob.getFilename(), "_temp.pdf");
            pdfDoc.save(file);

        } finally {
            pdfDoc.close();
        }

        return file;
    }

    private void writePage(PDDocument pdfDoc, PDPage page)
            throws IOException, EloraException {

        PDPageContentStream cs = new PDPageContentStream(pdfDoc, page, true,
                true, true);
        page = addGraphicsState(page, OPAQUE_GS_ID, 1f);

        try {

            // Lines, rects and images first because text must be above them

            if (modifier.lines != null && modifier.lines.length > 0) {

                for (ModifierLineDescriptor line : modifier.lines) {
                    if (line != null) {
                        processLine(cs, page, line);
                    }
                }
            }

            if (modifier.rects != null && modifier.rects.length > 0) {

                for (int r = 0; r < modifier.rects.length; r++) {
                    ModifierRectDescriptor rect = modifier.rects[r];
                    if (rect != null) {
                        processRect(cs, page, r, rect);
                    }
                }
            }

            if (modifier.images != null && modifier.images.length > 0) {

                for (int i = 0; i < modifier.images.length; i++) {
                    ModifierImageDescriptor image = modifier.images[i];
                    if (image != null) {
                        processImage(pdfDoc, cs, page, i, image);
                    }
                }

            }

            if (modifier.texts != null && modifier.texts.length > 0) {

                for (int t = 0; t < modifier.texts.length; t++) {
                    ModifierTextDescriptor text = modifier.texts[t];
                    if (text != null) {
                        processText(cs, page, t, text);
                    }
                }
            }

        } finally {
            cs.close();
        }
    }

    private PDPage addGraphicsState(PDPage page, String id, float opacity) {
        if (page.getResources() == null) {
            page.setResources(new PDResources());
        }
        Map<String, PDExtendedGraphicsState> gsMap = page.getResources().getGraphicsStates();
        if (gsMap == null) {
            gsMap = new HashMap<String, PDExtendedGraphicsState>();
        }

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(opacity);

        gsMap.put(id, gs);
        page.getResources().setGraphicsStates(gsMap);

        return page;
    }

    private void processText(PDPageContentStream cs, PDPage page, int t,
            ModifierTextDescriptor text) throws EloraException, IOException {

        if (ConditionEvaluatorHelper.fulfillsConditions(currentDoc, relatedDoc,
                action, text.conditions, text.allConditionsRequired)) {

            String value = getTextValue(currentDoc, relatedDoc, text);

            if (value != null && !value.isEmpty()) {
                cs.setFont(text.font, text.size);
                cs.setNonStrokingColor(text.color);

                if (text.opacity != null && text.opacity != 1f) {
                    page = addGraphicsState(page, "gs" + t,
                            text.opacity.floatValue());
                    cs.appendRawCommands("/gs" + t + " gs\n");
                } else {
                    cs.appendRawCommands("/" + OPAQUE_GS_ID + " gs\n");
                }

                cs.beginText();
                cs = rotateAndSetPosition(cs, page, text, value);
                cs.drawString(value);
                cs.endText();
            }
        }
    }

    private String getTextValue(DocumentModel doc, DocumentModel relatedDoc,
            ModifierTextDescriptor text) throws EloraException {

        DataObtainerAdapter doa = new DataObtainerAdapter(text);
        String value = ValueObtainerHelper.getValue(doc, relatedDoc, action,
                doa);
        if (text.postProcessor != null) {
            value = PostProcessingHelper.callPostProcessor(text.postProcessor,
                    value);
        }
        return value;
    }

    private float getRealXPosition(float refPointX, PDFont font, String value,
            String align, int size, int x) throws IOException {

        float textWidth = (font.getStringWidth(value) / 1000.0f) * size;
        switch (align) {

        case ALIGN_OPTION_LEFT:
            return refPointX + x;

        case ALIGN_OPTION_RIGHT:
            return refPointX + x - textWidth;

        case ALIGN_OPTION_CENTER:
            return refPointX + x - (textWidth / 2);

        }
        return 0;
    }

    private float getRealYPosition(float refPointY, int y) throws IOException {
        return refPointY + y;
    }

    private PDPageContentStream rotateAndSetPosition(PDPageContentStream cs,
            PDPage page, ModifierTextDescriptor text, String value)
            throws IOException {

        Float[] refPoint = getRefPoint(page, text.refPointOption);

        if (text.rotation != null && text.rotation != 0) {
            cs.setTextRotation(Math.toRadians(text.rotation),
                    refPoint[0] + text.x, refPoint[1] + text.y);

            cs.moveTextPositionByAmount(getRotationTx(text, value), 0);

        } else {
            float realPosX = getRealXPosition(refPoint[0], text.font, value,
                    text.align, text.size, text.x);
            float realPosY = getRealYPosition(refPoint[1], text.y);
            cs.moveTextPositionByAmount(realPosX, realPosY);
        }

        return cs;
    }

    private Float[] getRefPoint(PDPage page, String[] refPointOption) {
        Float[] refPoint = { 0f, 0f };

        PDRectangle mbox = page.getMediaBox();
        if (mbox == null) {
            mbox = page.findMediaBox();
        }

        float pageWidth = mbox.getWidth();
        float pageHeight = mbox.getHeight();

        String refPointXOption = refPointOption != null
                && refPointOption[0] != null ? refPointOption[0]
                        : modifier.defaultRefPointOption[0];
        String refPointYOption = refPointOption != null
                && refPointOption[1] != null ? refPointOption[1]
                        : modifier.defaultRefPointOption[1];

        switch (refPointXOption) {
        case REF_POINT_X_OPTION_CENTER:
            refPoint[0] = pageWidth / 2;
            break;
        case REF_POINT_X_OPTION_RIGHT:
            refPoint[0] = pageWidth;
            break;
        case REF_POINT_X_OPTION_LEFT:
        default:
            refPoint[0] = 0f;
            break;
        }

        switch (refPointYOption) {
        case REF_POINT_Y_OPTION_TOP:
            refPoint[1] = pageHeight;
            break;
        case REF_POINT_Y_OPTION_CENTER:
            refPoint[1] = pageHeight / 2;
            break;
        case REF_POINT_Y_OPTION_BOTTOM:
        default:
            refPoint[1] = 0f;
            break;
        }

        return refPoint;
    }

    private float getRotationTx(ModifierTextDescriptor text, String value)
            throws IOException {

        float tx = 0;

        float stringWidth = text.font.getStringWidth(value);
        switch (text.align) {
        case ALIGN_OPTION_LEFT:
            tx = 0;
            break;
        case ALIGN_OPTION_CENTER:
            tx = -((stringWidth * text.size) / 1000f) / 2;
            break;
        case ALIGN_OPTION_RIGHT:
            tx = -((stringWidth * text.size) / 1000f);
            break;
        }

        return tx;
    }

    private void processLine(PDPageContentStream cs, PDPage page,
            ModifierLineDescriptor line) throws IOException, EloraException {

        if (ConditionEvaluatorHelper.fulfillsConditions(currentDoc, relatedDoc,
                action, line.conditions, line.allConditionsRequired)) {

            Float[] refPoint0 = getRefPoint(page, line.refPointOption0);
            Float[] refPoint1 = getRefPoint(page, line.refPointOption1);

            cs.setLineWidth(line.width.floatValue());
            cs.setStrokingColor(line.color);
            cs.drawLine(refPoint0[0] + line.x0, refPoint0[1] + line.y0,
                    refPoint1[0] + line.x1, refPoint1[1] + line.y1);
        }
    }

    private void processRect(PDPageContentStream cs, PDPage page, int r,
            ModifierRectDescriptor rect) throws IOException, EloraException {

        if (ConditionEvaluatorHelper.fulfillsConditions(currentDoc, relatedDoc,
                action, rect.conditions, rect.allConditionsRequired)) {

            Float[] refPoint = getRefPoint(page, rect.refPointOption);

            if (rect.opacity != null && rect.opacity != 1f) {
                page = addGraphicsState(page, "gs" + r,
                        rect.opacity.floatValue());
                cs.appendRawCommands("/gs" + r + " gs\n");
            } else {
                cs.appendRawCommands("/" + OPAQUE_GS_ID + " gs\n");
            }

            cs.addRect(refPoint[0] + rect.x, refPoint[1] + rect.y, rect.width,
                    rect.height);
            if (rect.lineColor != null) {
                cs.setStrokingColor(rect.lineColor);
                cs.setLineDashPattern(new float[] {}, 0);
                cs.setLineWidth(rect.lineWidth.floatValue());
                cs.stroke();
            }

            if (rect.fillColor != null) {
                cs.setNonStrokingColor(rect.fillColor);
                cs.fillRect(refPoint[0] + rect.x, refPoint[1] + rect.y,
                        rect.width, rect.height);
            }
        }

    }

    private void processImage(PDDocument document, PDPageContentStream cs,
            PDPage page, int i, ModifierImageDescriptor image)
            throws IOException, EloraException {

        if (ConditionEvaluatorHelper.fulfillsConditions(currentDoc, relatedDoc,
                action, image.conditions, image.allConditionsRequired)) {

            Float[] refPoint = getRefPoint(page, image.refPointOption);

            if (image.opacity != null && image.opacity != 1f) {
                page = addGraphicsState(page, "gs" + i,
                        image.opacity.floatValue());
                cs.appendRawCommands("/gs" + i + " gs\n");
            } else {
                cs.appendRawCommands("/" + OPAQUE_GS_ID + " gs\n");
            }

            URL url = this.getClass().getClassLoader().getResource(
                    "web/nuxeo.war" + image.path);
            BufferedImage tmp_img = ImageIO.read(url);
            BufferedImage buff_img = new BufferedImage(tmp_img.getWidth(),
                    tmp_img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            buff_img.createGraphics().drawRenderedImage(tmp_img, null);
            PDXObjectImage x_img = new PDPixelMap(document, buff_img);

            cs.drawXObject(x_img, refPoint[0] + image.x, refPoint[1] + image.y,
                    x_img.getWidth() * image.scale.floatValue(),
                    x_img.getHeight() * image.scale.floatValue());
        }

    }

}
