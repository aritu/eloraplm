/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "modifier")
public class ModifierDescriptor {

    @XNode("@id")
    public String id;

    public String[] defaultRefPointOption = { "left", "bottom" };

    @XNode("@defaultRefPoint")
    public void setRefPoint(String xy) {
        if (xy != null) {
            String[] sxy = xy.split(",");
            if (sxy.length != 2) {
                throw new IllegalArgumentException(
                        "defaultRefPoint format must be x,y");
            }
            defaultRefPointOption = sxy;
        }
    }

    @XNodeList(value = "line", type = ModifierLineDescriptor[].class, componentType = ModifierLineDescriptor.class)
    public ModifierLineDescriptor[] lines;

    @XNodeList(value = "text", type = ModifierTextDescriptor[].class, componentType = ModifierTextDescriptor.class)
    public ModifierTextDescriptor[] texts;
}
