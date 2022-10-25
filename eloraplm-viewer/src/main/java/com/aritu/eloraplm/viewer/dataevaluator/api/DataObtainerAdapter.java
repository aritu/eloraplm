/**
 *
 */
package com.aritu.eloraplm.viewer.dataevaluator.api;

import com.aritu.eloraplm.viewer.api.ModifierTextDescriptor;
import com.aritu.eloraplm.viewer.filename.api.NamePartDescriptor;

/**
 * This adapter allows to store the important information that is needed when we
 * want to get data from the target document, without depending on the
 * descriptor type that has the details about the needed information.
 *
 * @author aritu
 *
 */

public class DataObtainerAdapter {

    private String type;

    private String xpath;

    private String method;

    private Object[] methodParams;

    private String value;

    private String source;

    public DataObtainerAdapter(Object descriptor) {
        if (descriptor instanceof ModifierTextDescriptor) {
            ModifierTextDescriptor d = (ModifierTextDescriptor) descriptor;
            type = d.type;
            xpath = d.xpath;
            method = d.method;
            methodParams = d.methodParams;
            value = d.value;
            source = d.source;
        } else if (descriptor instanceof ConditionDescriptor) {
            ConditionDescriptor d = (ConditionDescriptor) descriptor;
            type = d.type;
            xpath = d.xpath;
            method = d.method;
            source = d.source;
        } else if (descriptor instanceof NamePartDescriptor) {
            NamePartDescriptor d = (NamePartDescriptor) descriptor;
            type = d.type;
            xpath = d.xpath;
            method = d.method;
            methodParams = d.methodParams;
            value = d.value;
        }
    }

    public String getType() {
        return type;
    }

    public String getXpath() {
        return xpath;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getMethodParams() {
        return methodParams;
    }

    public String getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

}
