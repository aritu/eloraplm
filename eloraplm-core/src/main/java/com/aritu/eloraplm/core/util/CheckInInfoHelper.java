/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package com.aritu.eloraplm.core.util;

import java.util.Date;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraMetadataConstants;

/**
 * Helper class for managing Check In Info metadata.
 *
 *
 * @author aritu
 *
 */
public class CheckInInfoHelper {

    public static void setLastCheckInInfoProperties(DocumentModel doc,
            String lastCheckedInBy, Date lastCheckedInDate) {
        doc.setPropertyValue(
                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY,
                lastCheckedInBy);
        doc.setPropertyValue(
                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE,
                lastCheckedInDate);
    }

    public static void setLastCheckInInfoProperties(DocumentModel doc,
            String lastCheckedInBy) {
        setLastCheckInInfoProperties(doc, lastCheckedInBy, new Date());
    }

    public static void emptyLastCheckInInfoProperties(DocumentModel doc) {
        doc.setPropertyValue(
                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY, null);
        doc.setPropertyValue(
                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE,
                null);
    }

}
