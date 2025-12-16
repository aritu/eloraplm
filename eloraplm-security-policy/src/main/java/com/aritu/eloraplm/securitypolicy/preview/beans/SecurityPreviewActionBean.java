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

package com.aritu.eloraplm.securitypolicy.preview.beans;

import java.io.Serializable;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.ConversionActionBean;

import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityFacetConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;
import com.aritu.eloraplm.webapp.preview.util.EloraPreviewHelper;

import static org.jboss.seam.annotations.Install.DEPLOYMENT;

/**
 *
 * @author aritu
 *
 */

@Name("eloraPreviewActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = DEPLOYMENT)
public class SecurityPreviewActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient ConversionActionBean conversionActions;

    public SecurityPreviewActionBean() {
    }

    public String getPreviewUrl(DocumentModel doc) {

        String xpath = null;
        Blob blob = null;

        if (doc.hasFacet(SecurityFacetConstants.FACET_HAS_SECURITY_INFO)) {
            xpath = SecurityPolicyMetadataConstants.SEC_SECURITY_VIEWER_FILE;
        } else if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            xpath = EloraSchemaConstants.ELORA_VIEWER;
        }

        if (xpath != null) {
            Blob viewerBlob = (Blob) doc.getPropertyValue(xpath);
            if (viewerBlob != null) {
                blob = viewerBlob;
            }
        }

        if (blob == null
                && doc.hasSchema(NuxeoSchemaConstants.MAIN_BLOB_SCHEMA)) {
            xpath = NuxeoMetadataConstants.NX_FILE_CONTENT;
            blob = (Blob) doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_FILE_CONTENT);
        }

        return getPreviewUrl(doc, xpath, blob);
    }

    public String getPreviewUrl(DocumentModel doc, String xpath, Blob blob) {

        if (xpath == null || blob == null) {
            return null;
        }

        if (PreviewHelper.isPDF(blob)
                || conversionActions.isExportableToPDF(blob)) {
            return EloraPreviewHelper.buildPreviewUrlWithViewer(doc, xpath,
                    blob);
        } else {
            return EloraPreviewHelper.buildPreviewUrlWithoutViewer(doc, xpath);
        }
    }

    public boolean isImage(Blob blob) {
        if (blob != null) {
            String mimetype = blob.getMimeType();
            if (mimetype.startsWith("image/")) {
                return true;
            }
        }

        return false;
    }

}
