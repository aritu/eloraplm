/**
 *
 */
package com.aritu.eloraplm.core.duplicating.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "duplicator")
public class DuplicatorDescriptor {

    @XNode("@id")
    public String id;

    // @XNode(value = "duplicator")
    // public String duplicatorClass;

}
