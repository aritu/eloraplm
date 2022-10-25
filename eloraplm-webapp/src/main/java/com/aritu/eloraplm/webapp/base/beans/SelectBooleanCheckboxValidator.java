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
package com.aritu.eloraplm.webapp.base.beans;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import com.sun.faces.util.MessageFactory;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 *
 * @author aritu
 *
 */
@Name("selectBooleanCheckboxValidator")
@Validator
@BypassInterceptors
public class SelectBooleanCheckboxValidator
        implements javax.faces.validator.Validator {

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        if (value instanceof Boolean && value.equals(Boolean.FALSE)) {
            String clientId = component.getClientId(context);
            Object[] params = { clientId };

            FacesMessage msg;
            String requiredMessageStr = null;
            if (component instanceof UIInput) {
                requiredMessageStr = ((UIInput) component).getRequiredMessage();
            }

            // respect the message string override on the component to emulate
            // required="true" behavior
            if (requiredMessageStr != null) {
                msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        requiredMessageStr, requiredMessageStr);
            } else {
                msg = MessageFactory.getMessage(context,
                        UIInput.REQUIRED_MESSAGE_ID,
                        MessageFactory.getLabel(context, component));
            }

            throw new ValidatorException(msg);
        }
    }

}
