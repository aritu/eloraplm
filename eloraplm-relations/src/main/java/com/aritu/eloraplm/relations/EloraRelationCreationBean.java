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

package com.aritu.eloraplm.relations;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import com.aritu.eloraplm.core.util.EloraMessageHelper;

/**
 * Helper for creation form validation and display.
 */
@Name("eloraRelationCreationBean")
public class EloraRelationCreationBean {

    private static final Log log = LogFactory.getLog(
            EloraRelationCreationBean.class);

    public void validateObject(FacesContext context, UIComponent component,
            Object value) {
        String logInitMsg = "[validateObject] ";

        log.trace(logInitMsg + "Entering doc validation...");
        Map<String, Object> attributes = component.getAttributes();
        final String objectDocumentUidInputId = (String) attributes.get(
                "objectDocumentUidInputId");

        log.trace(logInitMsg + "Value: |" + objectDocumentUidInputId + "|");

        if (StringUtils.isBlank(objectDocumentUidInputId)) {
            log.error("Cannot validate relation creation: input id not found");
            return;
        }

        FacesMessage message;
        final UIInput objectDocumentUidInput = (UIInput) component.findComponent(
                objectDocumentUidInputId);
        if (objectDocumentUidInput == null) {
            String msg = EloraMessageHelper.getTranslatedMessage(context,
                    "error.relation.required.object.document");
            message = new FacesMessage(msg);
        }

        String objectValue = ((String) objectDocumentUidInput.getLocalValue());
        log.trace(logInitMsg + "Local value: |" + objectValue + "|");

        if (objectValue != null) {
            objectValue = objectValue.trim();
        }
        if (objectValue == null || objectValue.length() == 0) {
            String msg = EloraMessageHelper.getTranslatedMessage(context,
                    "error.relation.required.object.document");
            message = new FacesMessage(msg);
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }

}
