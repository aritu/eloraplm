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

@XObject(value = "condition")
public class ArchiverConditionDescriptor {

    @XNode("@class")
    public String conditionClass;

    @XNode("@method")
    public String method;

}
