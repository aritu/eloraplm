package com.aritu.eloraplm.webapp.base.convert;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import com.aritu.eloraplm.core.util.EloraDecimalHelper;

/**
 * This class validates a given measure value.
 *
 * @author aritu
 *
 */
@Name("decimalValueValidator")
@Validator
@BypassInterceptors
public class DecimalValueValidator implements javax.faces.validator.Validator {

    private static final Log log = LogFactory
            .getLog(DecimalValueValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        Integer maxIntegers = component.getAttributes()
                .get("maxIntegerPlaces") != null
                        ? (int) (long) component.getAttributes()
                                .get("maxIntegerPlaces")
                        : null;
        Integer maxDecimals = component.getAttributes()
                .get("maxDecimalPlaces") != null
                        ? (int) (long) component.getAttributes()
                                .get("maxDecimalPlaces")
                        : null;

        UIInput input = (UIInput) component;
        String submittedValue = (String) input.getSubmittedValue();
        String convertedValue = (String) value;

        Locale locale = context.getViewRoot().getLocale();
        EloraDecimalHelper.validateDecimalValue(locale, submittedValue,
                convertedValue, maxIntegers, maxDecimals);

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
