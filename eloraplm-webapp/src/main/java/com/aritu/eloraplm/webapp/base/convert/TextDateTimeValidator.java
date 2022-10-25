package com.aritu.eloraplm.webapp.base.convert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import com.sun.faces.util.MessageFactory;

/**
 * This class validates a given date in string comparing it with the format.
 *
 * @author aritu
 *
 */
@Name("textDateTimeValidator")
@Validator
@BypassInterceptors
public class TextDateTimeValidator implements javax.faces.validator.Validator {

    private static final Log log = LogFactory.getLog(
            TextDateTimeValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        String dateStr = (String) value;

        if (dateStr != null && !dateStr.isEmpty()) {
            String format = (String) component.getAttributes().get("format");
            boolean withTime = (boolean) component.getAttributes().get(
                    "withTime");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            try {
                if (withTime) {
                    LocalDateTime.parse(dateStr, dtf);
                } else {
                    LocalDate.parse(dateStr, dtf);
                }
            } catch (DateTimeParseException e) {
                FacesMessage message = MessageFactory.getMessage(context,
                        "eloraplm.message.error.textDateTimeValidator", value);

                throw new ValidatorException(message);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
