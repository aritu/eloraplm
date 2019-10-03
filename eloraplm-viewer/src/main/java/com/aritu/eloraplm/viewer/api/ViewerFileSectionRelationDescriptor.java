/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "relation")
public class ViewerFileSectionRelationDescriptor {

    @XNode("@inverse")
    public Boolean inverse = false;

    @XNode("@predicate")
    public String predicate;

    @XNode("@xpath")
    public String xpath;

    @XNode("@template")
    public String template;

    @XNode("@modifier")
    public String modifier;

    @XNode("@checkImportationDateForOverwriteViewer")
    public Boolean checkImportationDateForOverwriteViewer = false;

}