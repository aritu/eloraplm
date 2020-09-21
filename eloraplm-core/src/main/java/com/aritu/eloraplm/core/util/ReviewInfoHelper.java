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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;

/**
 * Helper class for managing Review Info metadata.
 *
 *
 * @author aritu
 *
 */
public class ReviewInfoHelper {

    private static final Log log = LogFactory.getLog(ReviewInfoHelper.class);

    public static void setLastReviewInfoProperties(DocumentModel doc,
            String lastReviewer) {
        doc.setPropertyValue(EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER,
                lastReviewer);
        doc.setPropertyValue(EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED,
                new Date());
    }

    public static void emptyLastReviewInfoProperties(DocumentModel doc) {
        doc.setPropertyValue(EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER,
                null);
        doc.setPropertyValue(EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED,
                null);
    }

    public static void setLastReviewInfoPropertiesByState(DocumentModel doc,
            String state, CoreSession session) {

        String logInitMsg = "[setLastReviewInfoPropertiesByState] ["
                + session.getPrincipal().getName() + "] ";

        String stateStatus = LifecyclesConfig.getStateStatus(state);
        log.trace(logInitMsg + "state = |" + state + "|, stateStatus = |"
                + stateStatus + "|");

        switch (stateStatus) {
        // If state status is RELEASED or OBSOLETE, fill last Review
        // Info properties
        case EloraLifeCycleConstants.STATUS_RELEASED:
        case EloraLifeCycleConstants.STATUS_OBSOLETE:
            ReviewInfoHelper.setLastReviewInfoProperties(doc,
                    session.getPrincipal().toString());
            log.trace("Last review info properties filled.");
            break;
        // Otherwise empty last Review Info Properties
        default:
            ReviewInfoHelper.emptyLastReviewInfoProperties(doc);
            log.trace("Last review info properties emptied.");
            break;
        }

    }

}
