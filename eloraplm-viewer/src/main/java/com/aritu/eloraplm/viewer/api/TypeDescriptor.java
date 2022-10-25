/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import com.aritu.eloraplm.viewer.dataevaluator.api.ConditionDescriptor;

/**
 * @author aritu
 *
 */

@XObject(value = "type")
public class TypeDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@viewerFile")
    public String viewerFile;

    @XNode("@filename")
    public String filename;

    @XNode(value = "conditions@allRequired")
    public Boolean allConditionsRequired = true;

    @XNodeList(value = "conditions/condition", type = ConditionDescriptor[].class, componentType = ConditionDescriptor.class)
    public ConditionDescriptor[] conditions;

}
