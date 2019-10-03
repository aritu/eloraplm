package com.aritu.eloraplm.bom.characteristics;

import java.io.Serializable;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsValidatorHelper;

/**
 * This class validates BOM Characteristic related values.
 *
 * @author aritu
 *
 */
@Name("bomCharacteristicValidator")
public class BomCharacteristicsValidator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsValidator.class);

    public void validateStringValue(FacesContext context, UIComponent component,
            Object value) {

        String logInitMsg = "[validateStringValue] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        if (value != null) {
            String stringValue = value.toString();

            Long maxLength = (Long) component.getAttributes().get("maxLength");

            BomCharacteristicsValidatorHelper.validateStringValue(context,
                    stringValue, maxLength);
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return;
    }

}
