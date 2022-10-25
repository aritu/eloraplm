/**
 *
 */
package com.aritu.eloraplm.core.archiver.api;

import java.util.ArrayList;
import java.util.List;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */

@XObject(value = "archiver")
public class ArchiverDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@destinationFolder")
    public String destinationFolder;

    @XNodeList(value = "condition", type = ArchiverConditionDescriptor[].class, componentType = ArchiverConditionDescriptor.class)
    public ArchiverConditionDescriptor[] conditions;

    @XNodeList(value = "executer", type = ArchiverExecuterDescriptor[].class, componentType = ArchiverExecuterDescriptor.class)
    public ArchiverExecuterDescriptor[] executers;

}
