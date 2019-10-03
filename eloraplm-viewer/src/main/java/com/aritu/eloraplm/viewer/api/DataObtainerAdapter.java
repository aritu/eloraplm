/**
 *
 */
package com.aritu.eloraplm.viewer.api;

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

    private String value;

    public DataObtainerAdapter(Object descriptor) {
        if (descriptor instanceof ModifierTextDescriptor) {
            ModifierTextDescriptor d = (ModifierTextDescriptor) descriptor;
            type = d.type;
            xpath = d.xpath;
            method = d.method;
            value = d.value;
        } else if (descriptor instanceof ConditionDescriptor) {
            ConditionDescriptor d = (ConditionDescriptor) descriptor;
            type = d.type;
            xpath = d.xpath;
            method = d.method;
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

    public String getValue() {
        return value;
    }

}