package com.aritu.eloraplm.viewer.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author aritu
 *
 */
@XObject(value = "types")
public class TypesDescriptor {

    @XNodeMap(key = "@name", value = "type", type = HashMap.class, componentType = String.class)
    public Map<String, String> types = new HashMap<String, String>();

}
