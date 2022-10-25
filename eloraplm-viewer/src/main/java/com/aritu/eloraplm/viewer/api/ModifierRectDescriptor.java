/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import java.awt.Color;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import com.aritu.eloraplm.viewer.dataevaluator.api.ConditionDescriptor;

/**
 * @author aritu
 *
 */

@XObject(value = "rect")
public class ModifierRectDescriptor {

    @XNode("@x")
    public Integer x;

    @XNode("@y")
    public Integer y;

    @XNode("@width")
    public Integer width;

    @XNode("@height")
    public Integer height;

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

    @XNode("@opacity")
    public Double opacity = 1d;

    @XNode("@lineWidth")
    public Double lineWidth = 1d;

    public Color lineColor;

    @XNode("@lineColor")
    public void setLineColor(String rgb) {
        if (rgb != null) {
            String[] srgb = rgb.split(",");
            if (srgb.length != 3) {
                throw new IllegalArgumentException(
                        "line color format must be r,g,b");
            }

            lineColor = new Color(Integer.parseInt(srgb[0]),
                    Integer.parseInt(srgb[1]), Integer.parseInt(srgb[2]));
        }
    }

    public Color fillColor;

    @XNode("@fillColor")
    public void setFillColor(String rgb) {
        if (rgb != null) {
            String[] srgb = rgb.split(",");
            if (srgb.length != 3) {
                throw new IllegalArgumentException(
                        "fill color format must be r,g,b");
            }

            fillColor = new Color(Integer.parseInt(srgb[0]),
                    Integer.parseInt(srgb[1]), Integer.parseInt(srgb[2]));
        }
    }

    @XNode(value = "conditions@allRequired")
    public Boolean allConditionsRequired = true;

    @XNodeList(value = "conditions/condition", type = ConditionDescriptor[].class, componentType = ConditionDescriptor.class)
    public ConditionDescriptor[] conditions;
}