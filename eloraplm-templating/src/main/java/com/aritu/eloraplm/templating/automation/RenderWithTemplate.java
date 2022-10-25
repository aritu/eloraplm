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
package com.aritu.eloraplm.templating.automation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.api.Framework;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.templating.api.TemplatingService;

/**
 *
 * @author aritu
 *
 */
@Operation(id = RenderWithTemplate.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Render with Template", description = "Render document using a template and return the blob.")
public class RenderWithTemplate {

    public static final String ID = "Elora.Plm.RenderWithTemplate";

    private static final Log log = LogFactory.getLog(RenderWithTemplate.class);

    @Param(name = "templateId", required = true)
    protected String templateId;

    @Context
    private CoreSession session;

    @OperationMethod
    public Blob run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod
    public Blob run(DocumentModel doc) throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Rendering blob with template |" + templateId
                + "| for document |" + doc.getId() + "|...");

        Blob blob = null;

        try {

            TemplatingService ts = Framework.getService(
                    TemplatingService.class);
            blob = ts.processTemplate(templateId, doc);

            log.trace(logInitMsg + "Blob created with template |" + templateId
                    + "|.");

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }

        return blob;
    }

}
