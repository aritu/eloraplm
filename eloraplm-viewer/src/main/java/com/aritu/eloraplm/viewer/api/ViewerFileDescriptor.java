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

@XObject(value = "viewerFile")
public class ViewerFileDescriptor {

    @XNode("@id")
    public String id;

    @XNodeList(value = "section", type = ViewerFileSectionDescriptor[].class, componentType = ViewerFileSectionDescriptor.class)
    public ViewerFileSectionDescriptor[] sections;

}
