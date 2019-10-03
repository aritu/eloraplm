/**
 *
 */
package com.aritu.eloraplm.viewer.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.api.ConditionDescriptor;
import com.aritu.eloraplm.viewer.api.DataObtainerAdapter;
import com.aritu.eloraplm.viewer.api.ModifierDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierLineDescriptor;
import com.aritu.eloraplm.viewer.api.ModifierTextDescriptor;

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

    private static final String CONDITION_EXPRESSION_EQUALS = "equals";

    private static final String CONDITION_EXPRESSION_NOT_EQUALS = "not equals";

    private Blob blob;

    private ModifierDescriptor modifier;

    private DocumentModel doc;

    private DocumentModel baseVersion;

    private String action;

    public PdfWriterHelper(Blob blob, ModifierDescriptor modifier,
            DocumentModel doc, String action) throws EloraException {
        if (blob == null) {
            throw new EloraException("Provided blob is null.");
        }

        if (modifier == null) {
            throw new EloraException("Provided ModifierDescriptor is null.");
        }

        if (doc == null) {
            throw new EloraException("Provided doc is null.");
        }

        if (action == null) {
            throw new EloraException("Provided action is null.");
        }

        this.blob = blob;
        this.modifier = modifier;
        this.doc = doc;
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

    private void writePage(PDDocument pdfDoc, PDPage page)
            throws IOException, EloraException {

        PDPageContentStream cs = new PDPageContentStream(pdfDoc, page, true,
                true);
        page = addGraphicsState(page, OPAQUE_GS_ID, 1f);

        try {

            if (modifier.texts != null && modifier.texts.length > 0) {

                for (int t = 0; t < modifier.texts.length; t++) {
                    ModifierTextDescriptor text = modifier.texts[t];
                    if (text != null) {
                        processText(cs, page, t, text);
                    }
                }
            }

            if (modifier.lines != null && modifier.lines.length > 0) {

                for (ModifierLineDescriptor line : modifier.lines) {
                    if (line != null) {
                        processLine(cs, page, line);
                    }
                }
            }

        } finally {
            cs.close();
        }
    }

    private PDPage addGraphicsState(PDPage page, String id, float opacity) {
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

        if (fulfillsConditions(text.conditions, text.allConditionsRequired)) {

            String value = getTextValue(doc, text);

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

    private boolean fulfillsConditions(ConditionDescriptor[] conditions,
            boolean allRequired) throws EloraException {
        if (conditions == null || conditions.length == 0) {
            return true;
        }

        boolean allOk = false;
        for (ConditionDescriptor condition : conditions) {
            DataObtainerAdapter doa = new DataObtainerAdapter(condition);
            String left = getValue(doc, doa);
            String right = condition.value;
            boolean ok = condition.expression.equals(CONDITION_EXPRESSION_EQUALS)
                    ? left.equals(right)
                    : !left.equals(right);
            if (allRequired && !ok) {
                return false;
            }
            allOk = allOk || ok;
        }
        return allOk;
    }

    private String getTextValue(DocumentModel doc, ModifierTextDescriptor text)
            throws EloraException {

        DataObtainerAdapter doa = new DataObtainerAdapter(text);
        String value = getValue(doc, doa);
        if (text.postProcessor != null) {
            value = PostProcessingHelper.callPostProcessor(text.postProcessor,
                    value);
        }
        return value;
    }

    private String getValue(DocumentModel doc, DataObtainerAdapter doa)
            throws EloraException {

        DocumentModel target = getTargetConsideringExceptions(doa);
        Object o = getValueConsideringExceptions(target, doa);
        String value = stringifyObject(o);
        return value;
    }

    private DocumentModel getTargetConsideringExceptions(
            DataObtainerAdapter doa) throws EloraException {
        boolean needsBase = false;
        if (action.equals(ViewerConstants.ACTION_OVERWRITE_AV)) {
            needsBase = false;
        } else if (doa.getMethod() != null
                && doa.getMethod().equals("getVersionLabel")) {
            needsBase = true;
        } else if (action.equals(ViewerConstants.ACTION_OVERWRITE)) {
            if (doa.getMethod() != null
                    && doa.getMethod().equals("getCurrentLifeCycleState")) {
                needsBase = true;
            } else if (doa.getXpath() != null
                    && (doa.getXpath().equals("dc:lastContributor")
                            || doa.getXpath().equals("dc:modified"))) {
                needsBase = true;
            }
        }
        if (needsBase) {
            loadBaseVersion();
            if (baseVersion != null) {
                return baseVersion;
            }
        }

        return doc;
    }

    private Object getValueConsideringExceptions(DocumentModel target,
            DataObtainerAdapter doa) throws EloraException {

        if (doa.getMethod() != null && doa.getMethod().equals("dc:modified")
                && !(action.equals(ViewerConstants.ACTION_OVERWRITE)
                        || action.equals(
                                ViewerConstants.ACTION_OVERWRITE_AV))) {
            return Calendar.getInstance();
        }

        switch (doa.getType()) {
        case "xpath":
            Serializable pty = target.getPropertyValue(doa.getXpath());
            if (pty != null) {
                return pty;
            }
        case "method":
            try {
                Method method = target.getClass().getMethod(doa.getMethod());
                return method.invoke(target);
            } catch (SecurityException | NoSuchMethodException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new EloraException(
                        "Error calling to method |" + doa.getMethod() + "|.",
                        e);
            }
        case "fixed":
            return doa.getValue();
        default:
            throw new EloraException(
                    "DataObtainerAdapter type must be xpath, method or fixed, and provided value was |"
                            + doa.getType() + "|.");
        }
    }

    private String stringifyObject(Object o) {
        if (o != null) {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof Long) {
                int i = (int) (long) o;
                return String.valueOf(i);
            } else if (o instanceof GregorianCalendar) {
                GregorianCalendar cal = (GregorianCalendar) o;
                cal.setTimeZone(TimeZone.getTimeZone("UTC"));
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                fmt.setCalendar(cal);
                return fmt.format(cal.getTime()) + " UTC";
            }
        }
        return null;
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

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

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
            ModifierLineDescriptor line) throws IOException {

        Float[] refPoint0 = getRefPoint(page, line.refPointOption0);
        Float[] refPoint1 = getRefPoint(page, line.refPointOption1);

        cs.setLineWidth(line.width.floatValue());
        cs.setStrokingColor(line.color);
        cs.drawLine(refPoint0[0] + line.x0, refPoint0[1] + line.y0,
                refPoint1[0] + line.x1, refPoint1[1] + line.y1);
    }

    private void loadBaseVersion() throws EloraException {
        if (baseVersion == null) {
            DocumentModel wcDoc = null;
            if (doc.isImmutable()) {
                wcDoc = doc.getCoreSession().getWorkingCopy(doc.getRef());
            } else {
                wcDoc = doc;
            }
            baseVersion = EloraDocumentHelper.getBaseVersion(wcDoc);
        }
    }

}
