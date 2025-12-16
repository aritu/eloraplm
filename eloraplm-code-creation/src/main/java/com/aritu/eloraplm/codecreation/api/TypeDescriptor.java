/**
 *
 */
package com.aritu.eloraplm.codecreation.api;

import java.util.Arrays;

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

    public String mode = CodeCreationService.CODE_CREATION_TYPE_MODE_MANUAL;

    public boolean editable = CodeCreationService.CODE_CREATION_TYPE_EDITABLE_FALSE;

    @XNode("@mode")
    public void setMode(String mode) {
        if (!mode.isEmpty() && Arrays.asList(
                CodeCreationService.CODE_CREATION_TYPE_MODES).contains(mode)) {
            this.mode = mode;
        } else {
            throw new IllegalArgumentException(
                    "mode must be manual, manualRequired, auto or autoIfEmpty");
        }
    }

    @XNode("@editable")
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
