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

@XObject(value = "relation")
public class AllowedRelationDescriptor {

    @XNode("@predicateUri")
    public String predicateUri;

    @XNode(value = "source")
    public AllowedRelationSourceOrTargetDescriptor source;

    @XNode(value = "target")
    public AllowedRelationSourceOrTargetDescriptor target;

}
