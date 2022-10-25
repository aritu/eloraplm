/**
 *
 */
package com.aritu.eloraplm.viewer.dataevaluator.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import com.aritu.eloraplm.constants.ViewerConstants;

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

    @XNode("@operator")
    public String operator;

    @XNode("@value")
    public String value;

    @XNode(value = "@source")
    public String source = ViewerConstants.SOURCE_CURRENT_DOC;
}
