/**
 *
 */
package com.aritu.eloraplm.templating.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "template")
public class TemplateDescriptor {

    @XNode("@id")
    public String id;

    @XNode("@name")
    public String name;

    @XNode("@mimetype")
    public String mimetype;
}
