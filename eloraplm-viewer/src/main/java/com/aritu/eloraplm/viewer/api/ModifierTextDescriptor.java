/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import java.awt.Color;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.viewer.dataevaluator.api.ConditionDescriptor;

/**
 * @author aritu
 *
 */

@XObject(value = "text")
public class ModifierTextDescriptor {

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

    @XNode(value = "@source")
    public String source = ViewerConstants.SOURCE_CURRENT_DOC;

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

    @XNode("@x")
    public Integer x;

    @XNode("@y")
    public Integer y;

    public String style = "regular";

    @XNode("@style")
    public void setStyle(String style) {
        this.style = style;
        if (style != null && style.equals("bold,italic")) {
            font = PDType1Font.HELVETICA_BOLD_OBLIQUE;
            return;
        }
        if (style != null && style.equals("bold")) {
            font = PDType1Font.HELVETICA_BOLD;
            return;
        }
        if (style != null && style.equals("italic")) {
            font = PDType1Font.HELVETICA_OBLIQUE;
            return;
        }
        font = PDType1Font.HELVETICA;
    }

    @XNode("@align")
    public String align;

    public Color color = new Color(0, 0, 0);

    @XNode("@color")
    public void setColor(String rgb) {
        if (rgb != null) {
            String[] srgb = rgb.split(",");
            int length = srgb.length;
            if (length != 3) {
                throw new IllegalArgumentException(
                        "color format must be r,g,b");
            }

            color = new Color(Integer.parseInt(srgb[0]),
                    Integer.parseInt(srgb[1]), Integer.parseInt(srgb[2]));
        }
    }

    @XNode("@opacity")
    public Double opacity = 1d;

    @XNode("@rotation")
    public Integer rotation = 0;

    @XNode("@size")
    public Integer size;

    @XNode("@postProcessor")
    public String postProcessor;

    @XNode(value = "conditions@allRequired")
    public Boolean allConditionsRequired = true;

    @XNodeList(value = "conditions/condition", type = ConditionDescriptor[].class, componentType = ConditionDescriptor.class)
    public ConditionDescriptor[] conditions;

    public PDFont font = PDType1Font.HELVETICA;

}
