/**
 *
 */
package com.aritu.eloraplm.core.lifecycles.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "config")
public class ConfigDescriptor {

    @XNode("@transition")
    public String transition;

    @XNode("@allowedByAllParentStates")
    public Boolean allowedByAllParentStates;

    @XNode("@allowsAllChildStates")
    public Boolean allowsAllChildStates;

}
