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

@XObject(value = "section")
public class ViewerFileSectionDescriptor {

    @XNode("@order")
    public Integer order;

    @XNode("@type")
    public String type;

    @XNode("@template")
    public String template;

    @XNode("@xpath")
    public String xpath;

    @XNode("@modifier")
    public String modifier;

    @XNodeList(value = "relation", type = ViewerFileSectionRelationDescriptor[].class, componentType = ViewerFileSectionRelationDescriptor.class)
    public ViewerFileSectionRelationDescriptor[] relations;

}
