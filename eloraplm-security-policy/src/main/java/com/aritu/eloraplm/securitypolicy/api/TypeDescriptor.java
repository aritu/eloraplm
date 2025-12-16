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

@XObject(value = "type")
public class TypeDescriptor {

    @XNode("@id")
    public String id;

    @XNode("@level")
    public Integer level;

    /*
     *  Merges other TypelDescriptors values with current.
     *
     * @param other
     */
    public void merge(TypeDescriptor other) {
        if (other != null && id.equals(other.id)) {
            level = other.level == null ? level : other.level;
        }
    }

}
