package com.aritu.eloraplm.thumbnails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.preview.adapter.PlainTextPreviewer;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class PdfPreviewer extends PlainTextPreviewer {

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();
        StringBuilder htmlPage = new StringBuilder();

        // TODO Hau ataratzeko ez dau funtziorik?
        String pdfPath = "/nuxeo/api/v1/id/" + dm.getId()
                + "/@blob/elovwr:file";

        String sourceMimeType = blob.getMimeType();

        String converterName = getConversionService().getConverterName(
                sourceMimeType, "text/html");
        if (converterName == null) {
            converterName = "any2html";
        }

        BlobHolder blobHolder2preview = new DocumentBlobHolder(dm,
                "elovwr:file");
        BlobHolder result;

        String content = null;
        List<Blob> additionalBlobs = new ArrayList<Blob>();
        try {
            result = getConversionService().convert(converterName,
                    blobHolder2preview, null);
            // setMimeType(result);

            List<Blob> blobs = result.getBlobs();
            boolean isFirst = true;
            Blob previewBlob = null;

            for (Blob blb : blobs) {
                if (isFirst) {
                    previewBlob = blb;
                    isFirst = false;
                } else {
                    additionalBlobs.add(blb);
                }
            }
            content = previewBlob.getString();

        } catch (ConversionException e) {
            throw new PreviewException(e.getMessage(), e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        htmlPage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"/>");
        htmlPage.append("<html>");
        htmlPage.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head>");
        htmlPage.append("<body>");
        htmlPage.append(htmlContent(pdfPath, content));
        htmlPage.append("</body></html>");

        Blob mainBlob = Blobs.createBlob(htmlPage.toString(), "text/html",
                "UTF-8", "index.html");

        blobResults.add(mainBlob);
        blobResults.addAll(additionalBlobs);
        return blobResults;
    }

    protected String htmlContent(String pdfPath, String content) {
        return "<object data=\"" + pdfPath
                + "\" type=\"application/pdf\" width=\"100%\" height=\"100%\">"
                + content + "</object>";
    }

    public ConversionService getConversionService() {
        return Framework.getService(ConversionService.class);
    }

}
