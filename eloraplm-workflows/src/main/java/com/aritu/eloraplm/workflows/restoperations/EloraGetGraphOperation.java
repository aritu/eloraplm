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

import java.util.Locale;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

import com.aritu.eloraplm.constants.EloraMimeTypeConstants;
import com.aritu.eloraplm.workflows.jsongraph.JsonGraphRoute;

/**
 *
 * @author aritu
 *
 */
@Operation(id = EloraGetGraphOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Get graph", description = "get graph nodes.", addToStudio = false)
public class EloraGetGraphOperation {
    public final static String ID = "Document.Routing.GetGraph";

    @Context
    protected OperationContext context;

    @Param(name = "routeDocId", required = true)
    protected String routeDocId;

    /***
     * @since 5.7
     */
    @Param(name = "language", required = false)
    protected String language;

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run() {
        // Fix for es_ES, etc.
        language = language.substring(0, 2);

        Locale locale = language != null && !language.isEmpty()
                ? new Locale(language)
                : Locale.ENGLISH;
        JsonGraphRoute unrestrictedRunner = new JsonGraphRoute(session,
                routeDocId, locale);
        String json = unrestrictedRunner.getJSON();
        return Blobs.createBlob(json.toString(),
                EloraMimeTypeConstants.APPLICATION_JSON);
    }

}
