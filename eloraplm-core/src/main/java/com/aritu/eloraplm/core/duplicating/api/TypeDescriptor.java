/**
 *
 */
package com.aritu.eloraplm.core.duplicating.api;

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

    @XNode("@duplicator")
    public String duplicator;

    @XNode("@enabled")
    public Boolean enabled = true;

    /*
     *  Merges other TypeDescriptor values with current.
     *
     * @param other
     */
    public void merge(TypeDescriptor other) {
        if (other != null && id.equals(other.id)) {
            duplicator = other.duplicator == null ? duplicator
                    : other.duplicator;
            enabled = other.enabled == null ? enabled : other.enabled;
        }
    }
}
