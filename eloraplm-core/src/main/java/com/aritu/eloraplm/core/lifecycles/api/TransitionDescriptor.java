/**
 *
 */
package com.aritu.eloraplm.core.lifecycles.api;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "transition")
public class TransitionDescriptor {

    @XNode("@lifecycle")
    public String lifecycle;

    @XNode("@name")
    public String name;

    @XNode("@visible")
    public Boolean visible;

    @XNode("@direction")
    public String direction;

    @XNodeList(value = "permissions/item", componentType = String.class, type = ArrayList.class)
    public List<String> permissionFilters;

    @XNode(value = "permissions@append")
    public boolean appendPermissions = false;

    /*
     *  Merges other TransitionDescriptors values with current.
     *
     * @param other
     */
    public void merge(TransitionDescriptor other) {
        if (other != null && name.equals(other.name)
                && lifecycle.equals(other.lifecycle)) {
            visible = other.visible == null ? visible : other.visible;
            direction = other.direction == null ? direction : other.direction;
            if (other.appendPermissions) {
                permissionFilters.addAll(other.permissionFilters);
            } else {
                permissionFilters = other.permissionFilters == null
                        || other.permissionFilters.isEmpty() ? permissionFilters
                                : other.permissionFilters;
            }
        }
    }
}
