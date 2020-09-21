package com.aritu.eloraplm.webapp.base.convert;

import java.math.BigDecimal;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;

import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.sun.faces.util.MessageFactory;

/**
 * This class validates a given measure value.
 *
 * @author aritu
 *
 */
@Name("measureValueValidator")
@Validator
@BypassInterceptors
public class MeasureValueValidator implements javax.faces.validator.Validator {

    private static final Log log = LogFactory.getLog(
            MeasureValueValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        UIInput input = (UIInput) component;
        String submittedValue = (String) input.getSubmittedValue();

        if (value != null) {
            try {
                Locale locale = context.getViewRoot().getLocale();
                BigDecimal valueAsDecimal = EloraDecimalHelper.fromLocalizedToDecimal(
                        locale, submittedValue);

                int decimalPlaces = valueAsDecimal.stripTrailingZeros().scale() > 0
                        ? valueAsDecimal.stripTrailingZeros().scale()
                        : 0;
                int integerPlaces = valueAsDecimal.stripTrailingZeros().precision()
                        - decimalPlaces;

                int maxIntegers = Integer.parseInt(
                        EloraConfig.generalConfigMap.get(
                                EloraConfigConstants.KEY_DECIMAL_MAX_INTEGER_PLACES));
                int maxDecimals = Integer.parseInt(
                        EloraConfig.generalConfigMap.get(
                                EloraConfigConstants.KEY_DECIMAL_MAX_DECIMAL_PLACES));

                if (integerPlaces > maxIntegers
                        || decimalPlaces > maxDecimals) {
                    log.trace(logInitMsg + "Validation failed: "
                            + submittedValue + " has more than " + maxIntegers
                            + " integers and/or " + maxDecimals + " decimals.");

                    FacesMessage message = MessageFactory.getMessage(context,
                            "eloraplm.message.error.decimalValueOutOfLimits",
                            maxIntegers, maxDecimals);

                    throw new ValidatorException(message);
                }
            } catch (ValidatorException e) {
                throw e;
            } catch (Exception e) {
                log.trace(logInitMsg
                        + "Validation failed: Exception thrown. Exception class = |"
                        + e.getClass() + "|, message: " + e.getMessage(), e);

                FacesMessage message = MessageFactory.getMessage(context,
                        "eloraplm.message.error.measureValueValidator",
                        submittedValue);

                throw new ValidatorException(message);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
