package com.aritu.eloraplm.webapp.base.convert;

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

    private static final Log log = LogFactory.getLog(
            DecimalValueValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        String maxIntegers = (String) component.getAttributes().get(
                "maxIntegerPlaces");
        String maxDecimals = (String) component.getAttributes().get(
                "maxDecimalPlaces");

        UIInput input = (UIInput) component;
        String submittedValue = (String) input.getSubmittedValue();
        String convertedValue = (String) value;

        EloraDecimalHelper.validateDecimalValue(context, submittedValue,
                convertedValue, maxIntegers, maxDecimals);

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
