package com.aritu.eloraplm.bom.characteristics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;

import com.aritu.eloraplm.constants.EloraBomCharacteristicsConstants;
import com.sun.faces.util.MessageFactory;

/**
 * This class validates BOM Characteristics related values.
 *
 * @author aritu
 *
 */
@Name("bomCharacteristicValidator")
public class BomCharacteristicValidator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicValidator.class);

    public void validateListDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateListDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a list. Otherwise, there is
        // nothing to be validated.
        // Find the UI component that stores the BOM Characteristic type
        UIInput typeTargetComponent = (UIInput) component.findComponent(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID);
        if (typeTargetComponent == null) {
            log.error(logInitMsg + "type UI element identified by |"
                    + EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID
                    + "| is not defined");
            log.trace(logInitMsg + "--- EXIT ---");
            FacesMessage message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.typeUiComponentIsUndefined");
            throw new ValidatorException(message);
        }
        String bomCharacType = (String) typeTargetComponent.getValue();
        log.trace(logInitMsg + "bomCharacType=|" + bomCharacType + "|");

        if (bomCharacType != null && bomCharacType.equals(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST)) {

            // Find the UI component that stores the list content
            UIInput listContentTargetComponent = (UIInput) component.findComponent(
                    EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_CONTENT_UI_ID);
            if (listContentTargetComponent == null) {
                log.error(logInitMsg + "list content UI element identified by |"
                        + EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_CONTENT_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.listContentUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Retrieve the list content
            ArrayList<HashMap<String, String>> listContentArrayList = (ArrayList<HashMap<String, String>>) listContentTargetComponent.getLocalValue();
            if (listContentArrayList == null
                    || listContentArrayList.isEmpty()) {
                log.error(logInitMsg + "list content is empty");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.listContentIsEmpty");
                throw new ValidatorException(message);
            }

            log.trace(logInitMsg + "value = |" + value + "|");
            if (value != null) {
                String listDefaultValue = value.toString();

                if (!listDefaultValue.isEmpty()) {
                    boolean isDefaultValueDefinedInList = isValueDefinedInList(
                            listDefaultValue, listContentArrayList);

                    if (!isDefaultValueDefinedInList) {
                        log.error(
                                logInitMsg + "defaultValue=|" + listDefaultValue
                                        + "| is not defined in list content = |"
                                        + listContentArrayList + "|");
                        log.trace(logInitMsg + "--- EXIT ---");
                        FacesMessage message = MessageFactory.getMessage(
                                context,
                                "message.error.bomcharac.valueNotDefinedInListContent",
                                listDefaultValue);
                        throw new ValidatorException(message);
                    }
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    public void validateStringDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateStringDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a string. Otherwise, there is
        // nothing to be validated.
        // Find the UI component that stores the BOM Characteristic type
        UIInput typeTargetComponent = (UIInput) component.findComponent(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID);
        if (typeTargetComponent == null) {
            log.error(logInitMsg + "type UI element identified by |"
                    + EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID
                    + "| is not defined");
            log.trace(logInitMsg + "--- EXIT ---");
            FacesMessage message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.typeUiComponentIsUndefined");
            throw new ValidatorException(message);
        }
        String bomCharacType = (String) typeTargetComponent.getValue();
        log.trace(logInitMsg + "bomCharacType=|" + bomCharacType + "|");

        if (bomCharacType != null && bomCharacType.equals(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING)) {

            // Find the UI component that stores the string max length
            UIInput stringMaxLengthTargetComponent = (UIInput) component.findComponent(
                    EloraBomCharacteristicsConstants.BOM_CHARAC_STRING_MAX_LENGTH_UI_ID);
            if (stringMaxLengthTargetComponent == null) {
                log.error(logInitMsg
                        + "string max length UI element identified by |"
                        + EloraBomCharacteristicsConstants.BOM_CHARAC_STRING_MAX_LENGTH_UI_ID
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

            BigDecimal stringMaxLengthBD = (BigDecimal) stringMaxLengthTargetComponent.getLocalValue();
            int stringMaxLength = stringMaxLengthBD.intValueExact();

            log.trace(
                    logInitMsg + "stringMaxLength = |" + stringMaxLength + "|");
            log.trace(logInitMsg + "value = |" + value + "|");

            if (value != null) {
                String stringDefaultValue = value.toString();

                if (!stringDefaultValue.isEmpty()) {

                    validateStringValue(context, stringDefaultValue,
                            stringMaxLength);
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    public void validateNumberDefaultValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateNumberDefaultValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // Verify that the selected type is a number. Otherwise, there is
        // nothing to be validated.
        // Find the UI component that stores the BOM Characteristic type
        UIInput typeTargetComponent = (UIInput) component.findComponent(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID);
        if (typeTargetComponent == null) {
            log.error(logInitMsg + "type UI element identified by |"
                    + EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID
                    + "| is not defined");
            log.trace(logInitMsg + "--- EXIT ---");
            FacesMessage message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.typeUiComponentIsUndefined");
            throw new ValidatorException(message);
        }
        String bomCharacType = (String) typeTargetComponent.getValue();
        log.trace(logInitMsg + "bomCharacType=|" + bomCharacType + "|");

        if (bomCharacType != null && bomCharacType.equals(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER)) {

            // Find the UI component that stores the number max length
            UIInput numberMaxLengthTargetComponent = (UIInput) component.findComponent(
                    EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_LENGTH_UI_ID);
            if (numberMaxLengthTargetComponent == null) {
                log.error(logInitMsg
                        + "number max length UI element identified by |"
                        + EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_LENGTH_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberMaxLengthUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Find the UI component that stores the number max decimal places
            UIInput numberMaxDecimalPlacesTargetComponent = (UIInput) component.findComponent(
                    EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_DECIMAL_PLACES_UI_ID);
            if (numberMaxDecimalPlacesTargetComponent == null) {
                log.error(logInitMsg
                        + "number max decimal places UI element identified by |"
                        + EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_DECIMAL_PLACES_UI_ID
                        + "| is not defined");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberMaxDecimalPlacesUiComponentIsUndefined");
                throw new ValidatorException(message);
            }

            // Retrieve the number max length and max decimal places
            if (numberMaxLengthTargetComponent.getLocalValue() == null
                    || numberMaxDecimalPlacesTargetComponent.getLocalValue() == null) {
                // Let required="true" do its job.
                return;
            }

            BigDecimal numberMaxLengthBD = (BigDecimal) numberMaxLengthTargetComponent.getLocalValue();
            int numberMaxLength = numberMaxLengthBD.intValueExact();
            BigDecimal numberMaxDecimalPlacesBD = (BigDecimal) numberMaxDecimalPlacesTargetComponent.getLocalValue();
            int numberMaxDecimalPlaces = numberMaxDecimalPlacesBD.intValueExact();

            log.trace(logInitMsg + "numberMaxLength = |" + numberMaxLength
                    + "|, numberMaxDecimalPlaces=|" + numberMaxDecimalPlaces
                    + "|");
            log.trace(logInitMsg + "value = |" + value + "|");

            if (value != null) {
                Double numberDefaultValue = (Double) value;
                validateNumberValue(context, numberDefaultValue,
                        numberMaxLength, numberMaxDecimalPlaces);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

    // ******************************************************************************
    // TODO:: Ondorengo zati amankomuna metodo batera atera beharko litzateke:
    /*// Verify that the selected type is a number. Otherwise, there is
        // nothing to be validated.
        // Find the UI component that stores the BOM Characteristic type
        UIInput typeTargetComponent = (UIInput) component.findComponent(
                EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID);
        if (typeTargetComponent == null) {
            log.error(logInitMsg + "type UI element identified by |"
                    + EloraBomCharacteristicsConstants.BOM_CHARAC_TYPE_UI_ID
                    + "| is not defined");
            log.trace(logInitMsg + "--- EXIT ---");
            FacesMessage message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.typeUiComponentIsUndefined");
            throw new ValidatorException(message);
        }
        String bomCharacType = (String) typeTargetComponent.getValue();
        log.trace(logInitMsg + "bomCharacType=|" + bomCharacType + "|");*/

    // ****************************************************************************
    // TODO::: change this method to "validateListValue"
    private boolean isValueDefinedInList(String value,
            ArrayList<HashMap<String, String>> listContentArrayList) {

        boolean isValueDefinedInList = false;

        for (int i = 0; i < listContentArrayList.size(); i++) {
            HashMap<String, String> listContentElements = listContentArrayList.get(
                    i);
            if (listContentElements.get(
                    EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_VALUE) != null) {
                String listValue = listContentElements.get(
                        EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_VALUE);
                if (listValue.equals(value)) {
                    isValueDefinedInList = true;
                    return isValueDefinedInList;
                }
            }
        }
        return isValueDefinedInList;
    }

    private void validateStringValue(FacesContext context, String stringValue,
            int maxLength) {

        String logInitMsg = "[validateStringValue] ";

        if (stringValue != null) {
            if (stringValue.length() > maxLength) {

                log.error(logInitMsg + "stringValue=|" + stringValue
                        + "| is longer than maxLength = |" + maxLength + "|");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.stringLengthTooLong",
                        maxLength);
                throw new ValidatorException(message);
            }
        }
    }

    private void validateNumberValue(FacesContext context, Double numberValue,
            int maxLength, int maxDecimalPlaces) {

        String logInitMsg = "[validateNumberValue] ";

        if (numberValue != null) {

            // String strValue =
            // numberValue.stripTrailingZeros().toPlainString();
            String numberValutStr = numberValue.toString();
            int index = numberValutStr.indexOf(".");
            int numberLength = index < 0 ? numberValutStr.length()
                    : numberValutStr.length() - 1;

            int decimalPlaces = index < 0 ? 0
                    : numberValutStr.length() - index - 1;
            String[] splitter = numberValutStr.split("\\.");

            if (numberLength > maxLength) {

                log.error(logInitMsg + "numberValue=|" + numberValue
                        + "| is longer than maxLength = |" + maxLength + "|");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberLengthTooLong",
                        maxLength);
                throw new ValidatorException(message);
            }

            if (decimalPlaces > maxDecimalPlaces) {
                log.error(logInitMsg + "numberValue=|" + numberValue
                        + "| has more decimal places than than maxDecimalPlaces = |"
                        + maxDecimalPlaces + "|");
                log.trace(logInitMsg + "--- EXIT ---");
                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.numberTooMuchDecimalPlaces",
                        maxDecimalPlaces);
                throw new ValidatorException(message);
            }
        }
    }

    /* // TODO::: metodo hau leku generikoago batera pasa.
    int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
        String string = bigDecimal.stripTrailingZeros().toPlainString();
        int index = string.indexOf(".");
        return index < 0 ? 0 : string.length() - index - 1;
    }*/

}
