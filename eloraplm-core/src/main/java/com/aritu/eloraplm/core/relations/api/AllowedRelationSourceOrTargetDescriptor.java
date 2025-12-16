/**
 *
 */
package com.aritu.eloraplm.core.relations.api;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject()
public class AllowedRelationSourceOrTargetDescriptor {

    @XNodeList(value = "type", type = AllowedRelationSourceOrTargetTypeDescriptor[].class, componentType = AllowedRelationSourceOrTargetTypeDescriptor.class)
    public AllowedRelationSourceOrTargetTypeDescriptor[] types;

    @XNodeList(value = "facet", type = String[].class, componentType = String.class)
    public String[] facets;

}
