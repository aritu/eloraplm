/**
 *
 */
package com.aritu.eloraplm.core.types.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "type")
public class TypeDescriptor {

    @XNode("@id")
    public String id;

    @XNode("@color")
    public String color = TypeConfigService.TYPE_DEFAULT_COLOR;
}
