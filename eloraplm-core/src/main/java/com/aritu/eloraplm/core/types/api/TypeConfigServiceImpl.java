/**
 *
 */
package com.aritu.eloraplm.core.types.api;

import java.util.HashMap;
import java.util.Map;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author aritu
 *
 */
public class TypeConfigServiceImpl extends DefaultComponent
        implements TypeConfigService {

    // private static final Log log = LogFactory.getLog(
    // TypeConfigServiceImpl.class);

    private static final String XP_TYPES = "types";

    private Map<String, TypeDescriptor> types;

    @Override
    public void activate(ComponentContext context) {
        types = new HashMap<String, TypeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        types = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_TYPES:
            TypeDescriptor td = (TypeDescriptor) contribution;
            if (td.id != null) {
                types.put(td.id, td);
            } else {
                throw new NuxeoException("Type sent without an id");
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    // ------------------------
    //
    // ------------------------

    @Override
    public String getColorForType(String type) {
        if (types.containsKey(type)) {
            TypeDescriptor td = types.get(type);
            return td.color;
        } else {
            // Return default color
            return TYPE_DEFAULT_COLOR;
        }
    }

}
