/**
 *
 */
package com.aritu.eloraplm.viewer.filename.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import com.aritu.eloraplm.viewer.dataevaluator.api.ConditionDescriptor;

/**
 * @author aritu
 *
 */

@XObject(value = "filename")
public class NamePartDescriptor {

    @XNode("@type")
    public String type;

    @XNode("@xpath")
    public String xpath;

    @XNode("@method")
    public String method;

    @XNodeList(value = "param", type = Object[].class, componentType = Object.class)
    public Object[] methodParams;

    @XNode("@value")
    public String value;

    @XNode("@postProcessor")
    public String postProcessor;

    @XNode(value = "conditions@allRequired")
    public Boolean allConditionsRequired = true;

    @XNodeList(value = "conditions/condition", type = ConditionDescriptor[].class, componentType = ConditionDescriptor.class)
    public ConditionDescriptor[] conditions;

}
