/**
 *
 */
package com.aritu.eloraplm.workflows.util;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

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

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {

        return TaskStatusHelper.convertStatus(context, value, true);
    }

    // This converter is for visualization only
    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        return null;
    }
}
