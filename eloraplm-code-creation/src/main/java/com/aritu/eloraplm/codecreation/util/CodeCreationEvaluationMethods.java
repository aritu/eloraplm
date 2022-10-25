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
package com.aritu.eloraplm.codecreation.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CodeCreationEvaluationMethods {

    protected static Log log = LogFactory.getLog(
            CodeCreationEvaluationMethods.class);

    public static Object getPropertyValue(DocumentModel doc,
            String propertyName) throws EloraException {

        String logInitMsg = "[getPropertyValue] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        Object propertyValue = null;
        try {

            propertyValue = doc.getPropertyValue(propertyName);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        return propertyValue;
    }
}
