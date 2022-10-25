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

    // We have to use isLockableSet in order to have a default value, and be
    // able to merge states XP definitions
    public boolean isLockableSet;

    public boolean lockable = true;

    @XNode("@lockable")
    public void setLockable(boolean lockable) {
        this.lockable = lockable;
        isLockableSet = true;
    }

    @XNode("@color")
    public String color;

    @XNode("@order")
    public Integer order;

    // We have to use isFinalStateSet in order to have a default value, and be
    // able to merge states XP definitions
    public boolean isFinalStateSet;

    public boolean finalState = false;

    @XNode("@finalState")
    public void setFinalState(boolean finalState) {
        this.finalState = finalState;
        isFinalStateSet = true;
    }

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
            lockable = other.isLockableSet ? other.lockable : lockable;
            color = other.color == null ? color : other.color;
            order = other.order == null ? order : other.order;
            finalState = other.isFinalStateSet ? other.finalState : finalState;
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
