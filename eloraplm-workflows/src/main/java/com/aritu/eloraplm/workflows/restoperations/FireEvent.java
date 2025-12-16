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
package com.aritu.eloraplm.workflows.restoperations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = FireEvent.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Send Event", description = "Send a Nuxeo event.")
public class FireEvent {

    public static final String ID = "Elora.Plm.FireEvent";

    @Context
    private CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "comment", required = false)
    protected String comment;

    @Param(name = "contextProperties", required = false)
    protected Properties contextProps;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws EloraException {
        Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
        ctxProperties.put("comment", comment);
        if (contextProps != null && !contextProps.isEmpty()) {
            for (Map.Entry<String, String> prop : contextProps.entrySet()) {
                ctxProperties.put(prop.getKey(), prop.getValue());
            }
        }
        EloraEventHelper.fireEvent(name, doc, ctxProperties);
        return doc;
    }

}
