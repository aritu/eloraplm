/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import java.awt.Color;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "line")
public class ModifierLineDescriptor {

    @XNode("@x0")
    public Integer x0;

    @XNode("@y0")
    public Integer y0;

    @XNode("@x1")
    public Integer x1;

    @XNode("@y1")
    public Integer y1;

    public String[] refPointOption0;

    @XNode("@refPoint0")
    public void setRefPoint0(String xy) {
        if (xy != null) {
            String[] sxy = xy.split(",");
            if (sxy.length != 2) {
                throw new IllegalArgumentException(
                        "refPoint0 format must be x,y");
            }
            refPointOption0 = sxy;
        }
    }

    public String[] refPointOption1;

    @XNode("@refPoint1")
    public void setRefPoint1(String xy) {
        if (xy != null) {
            String[] sxy = xy.split(",");
            if (sxy.length != 2) {
                throw new IllegalArgumentException(
                        "refPoint1 format must be x,y");
            }
            refPointOption1 = sxy;
        }
    }

    @XNode("@width")
    public Double width = 1d;

    public Color color = new Color(0, 0, 0);

    @XNode("@color")
    public void setColor(String rgb) {
        if (rgb != null) {
            String[] srgb = rgb.split(",");
            if (srgb.length != 3) {
                throw new IllegalArgumentException(
                        "color format must be r,g,b");
            }

            color = new Color(Integer.parseInt(srgb[0]),
                    Integer.parseInt(srgb[1]), Integer.parseInt(srgb[2]));
        }
    }
}