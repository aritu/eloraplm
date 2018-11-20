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
package com.aritu.eloraplm.core.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author aritu
 *
 */
public class EloraEventHelper {

    // private static final Log log = LogFactory.getLog(EloraEventHelper.class);

    public static void fireEvent(String eventName, DocumentModel doc) {
        String comment = null;
        fireEvent(eventName, doc, comment);
    }

    public static void fireEvent(String eventName, DocumentModel doc,
            String comment) {
        Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
        ctxProperties.put("comment", comment);
        fireEvent(eventName, doc, ctxProperties);
    }

    public static void fireEvent(String eventName, DocumentModel doc,
            Map<String, Serializable> ctxProperties) {
        CoreSession session = doc.getCoreSession();
        // String logInitMsg = "[fireEvent] [" +
        // session.getPrincipal().getName()
        // + "] ";
        //
        EventProducer eventProducer = Framework.getService(EventProducer.class);
        DocumentEventContext ctx = new DocumentEventContext(session,
                session.getPrincipal(), doc);
        if (ctxProperties != null) {
            ctx.setProperties(ctxProperties);
        }
        Event event = ctx.newEvent(eventName);
        eventProducer.fireEvent(event);
    }

}
