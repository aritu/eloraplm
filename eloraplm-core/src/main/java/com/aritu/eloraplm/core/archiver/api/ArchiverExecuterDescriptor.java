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

@XObject(value = "executer")
public class ArchiverExecuterDescriptor {

    @XNode("@class")
    public String executerClass;

    @XNode("@method")
    public String method;

    public String type = WorkspaceArchiverService.EXECUTER_TYPE_POST;

    @XNode("@type")
    public void setType(String type) {
        if (!type.equals(WorkspaceArchiverService.EXECUTER_TYPE_POST)
                && !type.equals(WorkspaceArchiverService.EXECUTER_TYPE_PRE)) {
            throw new IllegalArgumentException("type must be pre or post");
        } else {
            this.type = type;

        }
    }

}
