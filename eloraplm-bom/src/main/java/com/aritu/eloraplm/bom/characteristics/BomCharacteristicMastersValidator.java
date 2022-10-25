package com.aritu.eloraplm.bom.characteristics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsValidatorHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.sun.faces.util.MessageFactory;

/**
 * This class validates BOM Characteristic Masters related values.
 *
 * @author aritu
 *
 */
@Name("bomCharacteristicMasterValidator")
public class BomCharacteristicMastersValidator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory
            .getLog(BomCharacteristicMastersValidator.class);

    public void validateMasterListDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateMasterListDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a list. Otherwise, there is
        // nothing to be validated.
        String bomCharacType = retrieveBomCharaMasterType(context, component);

        if (bomCharacType != null && bomCharacType
                .equals(BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST)) {

            // Find the UI component that stores the list content
            UIInput listContentTargetComponent = (UIInput) component
                    .findComponent(
                            BomCharacteristicsConstants.BOM_CHARAC_LIST_CONTENT_UI_ID);
            if (listContentTargetComponent == null) {
                log.error(logInitMsg + "list content UI element identified by |"
                        + BomCharacteristicsConstants.BOM_CHARAC_LIST_CONTENT_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.listContentUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Retrieve the list content
            @SuppressWarnings("unchecked")
            List<Map<String, String>> listContent = (List<Map<String, String>>) listContentTargetComponent
                    .getLocalValue();
            // TODO::: hau kenduta ondo????
            /*if (listContent == null || listContent.isEmpty()) {
                log.error(logInitMsg + "list content is empty");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.listContentIsEmpty");
                throw new ValidatorException(message);
            }*/

            log.trace(logInitMsg + "value = |" + value + "|");
            if (value != null) {
                String listDefaultValue = value.toString();

                BomCharacteristicsValidatorHelper.validateListValue(context,
                        listDefaultValue, listContent);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    public void validateMasterStringDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateMasterStringDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a string. Otherwise, there is
        // nothing to be validated.
        String bomCharacType = retrieveBomCharaMasterType(context, component);

        if (bomCharacType != null && bomCharacType
                .equals(BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING)) {

            // Find the UI component that stores the string max length
            UIInput stringMaxLengthTargetComponent = (UIInput) component
                    .findComponent(
                            BomCharacteristicsConstants.BOM_CHARAC_STRING_MAX_LENGTH_UI_ID);
            if (stringMaxLengthTargetComponent == null) {
                log.error(logInitMsg
                        + "string max length UI element identified by |"
                        + BomCharacteristicsConstants.BOM_CHARAC_STRING_MAX_LENGTH_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.stringMaxLengthUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Retrieve the string max length
            if (stringMaxLengthTargetComponent.getLocalValue() == null) {
                // Let required="true" do its job.
                return;
            }

            BigDecimal stringMaxLengthBD = (BigDecimal) stringMaxLengthTargetComponent
                    .getLocalValue();
            Long stringMaxLength = new Long(stringMaxLengthBD.longValueExact());

            log.trace(
                    logInitMsg + "stringMaxLength = |" + stringMaxLength + "|");
            log.trace(logInitMsg + "value = |" + value + "|");

            if (value != null) {
                String stringDefaultValue = value.toString();

                BomCharacteristicsValidatorHelper.validateStringValue(context,
                        stringDefaultValue, stringMaxLength);

            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    public void validateMasterNumberDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateMasterNumberDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a number. Otherwise, there is
        // nothing to be validated.
        String bomCharacType = retrieveBomCharaMasterType(context, component);

        if (bomCharacType != null && bomCharacType
                .equals(BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER)) {

            // Find the UI component that stores the number max integer places
            UIInput numberMaxIntegerPlacesTargetComponent = (UIInput) component
                    .findComponent(
                            BomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_INTEGER_PLACES_UI_ID);
            if (numberMaxIntegerPlacesTargetComponent == null) {
                log.error(logInitMsg
                        + "number max length UI element identified by |"
                        + BomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_INTEGER_PLACES_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberMaxIntegerPlacesUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Find the UI component that stores the number max decimal places
            UIInput numberMaxDecimalPlacesTargetComponent = (UIInput) component
                    .findComponent(
                            BomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_DECIMAL_PLACES_UI_ID);
            if (numberMaxDecimalPlacesTargetComponent == null) {
                log.error(logInitMsg
                        + "number max decimal places UI element identified by |"
                        + BomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_DECIMAL_PLACES_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberMaxDecimalPlacesUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Retrieve the number max integer places and max decimal places
            if (numberMaxIntegerPlacesTargetComponent.getLocalValue() == null
                    || numberMaxDecimalPlacesTargetComponent
                            .getLocalValue() == null) {
                // Let required="true" do its job.
                return;
            }

            Integer numberMaxIntegerPlaces = ((BigDecimal) numberMaxIntegerPlacesTargetComponent
                    .getLocalValue()).intValueExact();
            Integer numberMaxDecimalPlaces = ((BigDecimal) numberMaxDecimalPlacesTargetComponent
                    .getLocalValue()).intValueExact();

            log.trace(logInitMsg + "numberMaxIntegerPlaces = |"
                    + numberMaxIntegerPlaces.toString()
                    + "|, numberMaxDecimalPlaces=|"
                    + numberMaxDecimalPlaces.toString() + "|");
            log.trace(logInitMsg + "value = |" + value + "|");

            if (value != null) {
                String numberDefaultValue = (String) value;
                Locale locale = context.getViewRoot().getLocale();
                EloraDecimalHelper.validateDecimalValue(locale,
                        numberDefaultValue, numberMaxIntegerPlaces,
                        numberMaxDecimalPlaces);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    private String retrieveBomCharaMasterType(FacesContext context,
            UIComponent component) {

        String logInitMsg = "[retrieveBomCharaMasterType] ";
        log.trace(logInitMsg + "--- ENTER ---");

        String bomCharacMasterType = null;

        // Find the UI component that stores the BOM Characteristic Master type
        UIInput typeTargetComponent = (UIInput) component.findComponent(
                BomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID);
        if (typeTargetComponent == null) {
            log.error(logInitMsg + "type UI element identified by |"
                    + BomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID
                    + "| is not defined");
            log.trace(logInitMsg + "--- EXIT ---");
            FacesMessage message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.typeUiComponentIsUndefined");
            throw new ValidatorException(message);
        }
        bomCharacMasterType = (String) typeTargetComponent.getValue();

        log.trace(logInitMsg + "--- EXIT --- with bomCharacMasterType=|"
                + bomCharacMasterType + "|");

        return bomCharacMasterType;

    }

}
