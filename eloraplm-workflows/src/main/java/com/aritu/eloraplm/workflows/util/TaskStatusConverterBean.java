/**
 *
 */
package com.aritu.eloraplm.workflows.util;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * @author aritu
 *
 */
@Name("taskStatusConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class TaskStatusConverterBean implements Converter {

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {

        boolean addStyle = (boolean) component.getAttributes().getOrDefault("addStyle", false);
        return TaskStatusHelper.convertStatus(context, value, addStyle);
    }

    // This converter is for visualization only
    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        return null;
    }
}
