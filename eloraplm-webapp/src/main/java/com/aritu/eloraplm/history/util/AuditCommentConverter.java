/**
 *
 */
package com.aritu.eloraplm.history.util;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * @author aritu
 *
 */
@Name("auditCommentConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class AuditCommentConverter implements Converter {

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        String initialValue = (String) value;
        // TODO Aldatu lehenengo lerroa versioning konfigurazioaren arabera.
        String convertedValue = initialValue.replaceAll(
                "^([_A-Z]*\\.[0-9\\+]*)", "<strong>$1</strong>").replaceAll(
                        "(#[^ ]*)",
                        "<span class=\"logClientName\">$1</span>").replaceAll(
                                "(@[^ ]*)",
                                "<span class=\"logProcessReference\">$1</span>");

        return convertedValue;
    }

    // This converter is for visualization only
    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        return null;
    }
}