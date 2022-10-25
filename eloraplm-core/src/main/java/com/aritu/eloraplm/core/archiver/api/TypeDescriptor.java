/**
 *
 */
package com.aritu.eloraplm.core.archiver.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "type")
public class TypeDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@archiver")
    public String archiver;

}
