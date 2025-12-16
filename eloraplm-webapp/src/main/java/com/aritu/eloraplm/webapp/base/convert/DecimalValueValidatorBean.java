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
public class DecimalValueValidatorBean implements javax.faces.validator.Validator {

    private static final Log log = LogFactory.getLog(
            DecimalValueValidatorBean.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        Integer maxIntegers = component.getAttributes().get(
                "maxIntegerPlaces") != null
                        ? ((Number) component.getAttributes().get(
                                "maxIntegerPlaces")).intValue()
                        : null;
        Integer maxDecimals = component.getAttributes().get(
                "maxDecimalPlaces") != null
                        ? ((Number) component.getAttributes().get(
                                "maxDecimalPlaces")).intValue()
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
