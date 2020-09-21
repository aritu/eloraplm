/**
 *
 */
package com.aritu.eloraplm.workflows.util;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import java.util.Arrays;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * @author aritu
 *
 */
@Name("taskStatusConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class TaskStatusConverter implements Converter {

    private static final String[] TASK_STATUS_OK = { "validate", "accept",
            "ok", "homologate", "homologate_conditionally" };
    
    private static final String[] TASK_STATUS_KO = { "reject", "cancel", "deny",
            "ko" };

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        String initialValue = (String) value;
        String convertedValue = initialValue.replaceAll("_", " ");
        if (Arrays.asList(TASK_STATUS_OK).contains(initialValue)) {
            convertedValue = "<span class=\"taskStatusOk\">" + convertedValue
                    + "</span>";
        } else if (Arrays.asList(TASK_STATUS_KO).contains(initialValue)) {
            convertedValue = "<span class=\"taskStatusKo\">" + convertedValue
                    + "</span>";
        } else {
            convertedValue = "<span class=\"taskStatusNormal\">"
                    + convertedValue + "</span>";
        }

        return convertedValue;
    }

    // This converter is for visualization only
    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        return null;
    }
}
