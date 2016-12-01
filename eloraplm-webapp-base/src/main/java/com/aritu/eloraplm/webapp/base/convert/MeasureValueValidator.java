package com.aritu.eloraplm.webapp.base.convert;

import javax.faces.application.FacesMessage;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;

import com.sun.faces.util.MessageFactory;

/**
 * This class validates a given measure value.
 *
 * @author aritu
 *
 */
@Name("measureValueValidator")
public class MeasureValueValidator implements Validator {

    private static final Log log = LogFactory.getLog(
            MeasureValueValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        if (value != null) {
            if (!(value instanceof Double)) {
                log.error(logInitMsg + "|" + value + "| is not a double.");

                FacesMessage message = MessageFactory.getMessage(context,
                        "eloraplm.message.error.measureValueValidator", value);

                throw new ValidatorException(message);
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
