/**
 *
 */
package com.aritu.eloraplm.securitypolicy.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "level")
public class LevelDescriptor {

    @XNode("@id")
    public Integer id;

    @XNode("@label")
    public String label;

    @XNode("@modifier")
    public String modifier;

    @XNode("@color")
    public String color;

    /*
     *  Merges other LevelDescriptors values with current.
     *
     * @param other
     */
    public void merge(LevelDescriptor other) {
        if (other != null && id.equals(other.id)) {
            label = other.label == null ? label : other.label;
            modifier = other.modifier == null ? modifier : other.modifier;
            color = other.color == null ? color : other.color;
        }
    }

}
