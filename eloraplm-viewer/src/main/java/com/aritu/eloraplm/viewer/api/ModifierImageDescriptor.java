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

@XObject(value = "line")
public class ModifierImageDescriptor {

    @XNode("@x")
    public Integer x;

    @XNode("@y")
    public Integer y;

    @XNode("@path")
    public String path;

    public String[] refPointOption;

    @XNode("@refPoint")
    public void setRefPoint(String xy) {
        if (xy != null) {
            String[] sxy = xy.split(",");
            if (sxy.length != 2) {
                throw new IllegalArgumentException(
                        "refPoint format must be x,y");
            }
            refPointOption = sxy;
        }
    }

    @XNode("@scale")
    public Double scale = 1d;

    @XNode("@opacity")
    public Double opacity = 1d;

    @XNode(value = "conditions@allRequired")
    public Boolean allConditionsRequired = true;

    @XNodeList(value = "conditions/condition", type = ConditionDescriptor[].class, componentType = ConditionDescriptor.class)
    public ConditionDescriptor[] conditions;

}