/**
 *
 */
package com.aritu.eloraplm.core.relations.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "type")
public class AllowedRelationSourceOrTargetTypeDescriptor {

    @XNode("@excludeInherited")
    public boolean excludeInherited = false;

    @XNode("")
    public String value;

}
