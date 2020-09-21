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

@XObject(value = "state")
public class StateDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@status")
    public String status;

    @XNode("@lockable")
    public Boolean lockable;

    @XNode("@color")
    public String color;

    @XNode("@order")
    public Integer order;

    @XNodeList(value = "supportedStates/state", componentType = String.class, type = ArrayList.class)
    public List<String> supportedStates;

    @XNode(value = "supportedStates@append")
    public boolean appendSupportedStates = false;

    /*
     *  Merges other StateDescriptors values with current.
     *
     * @param other
     */
    public void merge(StateDescriptor other) {
        if (other != null && name.equals(other.name)) {
            status = other.status == null ? status : other.status;
            lockable = other.lockable == null ? lockable : other.lockable;
            color = other.color == null ? color : other.color;
            order = other.order == null ? order : other.order;
            if (other.appendSupportedStates) {
                supportedStates.addAll(other.supportedStates);
            } else {
                supportedStates = other.supportedStates == null
                        || other.supportedStates.isEmpty() ? supportedStates
                                : other.supportedStates;
            }
        }
    }

}
