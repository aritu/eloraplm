/**
 *
 */
package com.aritu.eloraplm.viewer.filename.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "filename")
public class FilenameDescriptor {

    @XNode("@id")
    public String id;

    @XNodeList(value = "namePart", type = NamePartDescriptor[].class, componentType = NamePartDescriptor.class)
    public NamePartDescriptor[] nameParts;

}
