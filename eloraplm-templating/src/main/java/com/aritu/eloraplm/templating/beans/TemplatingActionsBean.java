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
package com.aritu.eloraplm.templating.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import com.aritu.eloraplm.templating.adapters.FakeTemplateBasedDocumentAdapter;

/**
 * @author aritu
 *
 */
@Name("templatingActions")
@Scope(CONVERSATION)
public class TemplatingActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public String render(String templateName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = new FakeTemplateBasedDocumentAdapter(
                currentDocument, "HistoryAndEbomTemplate");
        try {
            Blob rendition = doc.renderWithTemplate(templateName);
            String filename = rendition.getFilename();
            ComponentUtils.download(currentDocument, null, rendition, filename,
                    "templateRendition");
            return null;
        } catch (NuxeoException e) {
            // log.error("Unable to render template ", e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.template.err.renderingFailed"));
            return null;
        }
    }

}
