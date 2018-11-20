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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.sun.faces.util.MessageFactory;

/**
 *
 * @author aritu
 *
 */
@Name("codeCreationConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class CodeCreationConverter implements Converter {

    private static final Log log = LogFactory.getLog(
            CodeCreationConverter.class);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {

        String logInitMsg = "[getAsObject / convertingValueToStore] ";

        DocumentModel doc = (DocumentModel) component.getAttributes().get(
                "document");

        boolean generateAutomaticCode = isGenerateAutomaticCode(doc, value);

        if (generateAutomaticCode) {
            CoreSession session = doc.getCoreSession();
            try {
                value = CodeCreationHelper.createCode(doc,
                        session.getPrincipal().getName());
            } catch (Exception e) {
                log.trace(logInitMsg
                        + "Conversion failed: Exception thrown. Exception class = |"
                        + e.getClass() + "|, message: " + e.getMessage(), e);

                FacesMessage message = MessageFactory.getMessage(context,
                        "eloraplm.message.error.codeCreationConverter");

                throw new ConverterException(message);
            }

        }

        return value;

    }

    /**
     * @param doc
     * @param value
     * @return
     */
    private boolean isGenerateAutomaticCode(DocumentModel doc, String value) {

        if (!doc.hasFacet(EloraFacetConstants.FACET_AUTOMATIC_CODE)) {
            return false;
        }

        if (value != null && !value.isEmpty()) {
            return false;
        }

        if (doc.getContextData(
                EloraGeneralConstants.CONTEXT_SKIP_AUTOMATIC_CODE_CREATION) != null) {

            boolean skipAutoCodeCreation = (boolean) doc.getContextData(
                    EloraGeneralConstants.CONTEXT_SKIP_AUTOMATIC_CODE_CREATION);

            if (skipAutoCodeCreation) {
                return false;
            }
        }

        return true;

    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        return (String) value;
    }

}
