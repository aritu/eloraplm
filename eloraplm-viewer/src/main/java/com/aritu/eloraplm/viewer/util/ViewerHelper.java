/**
 *
 */
package com.aritu.eloraplm.viewer.util;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

/**
 * @author aritu
 *
 */
public class ViewerHelper {

    public static final String PDF_MIMETYPE = "application/pdf";

    public static Blob getViewerBlob(DocumentModel doc) {
        if (doc != null && doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELOVWR_FILE) != null) {
            return (Blob) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELOVWR_FILE);
        }

        return null;
    }

    /**
     * Returns the viewer file name for the doc. If reference is filled,
     * filename will be reference_versionLabel.pdf. Otherwise,filename will be
     * docTitle_versionLabel.pdf.
     *
     * @param doc document which viewer is being created.
     * @return the viewer file name.
     */
    public static String getViewerFileName(DocumentModel doc) {
        String fileNamePrefix = (String) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        if (fileNamePrefix == null || fileNamePrefix.length() == 0) {
            fileNamePrefix = (String) doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_TITLE);
        }
        String formatedVersionLabel = getFormattedVersionLabel(doc);
        return fileNamePrefix + "_" + formatedVersionLabel + ".pdf";
    }

    private static String getFormattedVersionLabel(DocumentModel doc) {
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

}
