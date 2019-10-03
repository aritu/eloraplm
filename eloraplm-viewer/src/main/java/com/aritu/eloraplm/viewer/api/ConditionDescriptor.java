/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "condition")
public class ConditionDescriptor {

    @XNode("@type")
    public String type;

    @XNode("@xpath")
    public String xpath;

    @XNode("@method")
    public String method;

    @XNode("@expression")
    public String expression;

    @XNode("@value")
    public String value;
}