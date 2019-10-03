/**
 *
 */
package com.aritu.eloraplm.viewer.util;

import java.util.Locale;

import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;

import com.aritu.eloraplm.constants.ViewerConstants;

/**
 * @author aritu
 *
 */
public class PostProcessingHelper {

    public static String callPostProcessor(String id, String value) {
        if (id != null) {
            switch (id) {
            case ViewerConstants.POST_PROCESSOR_RESOLVE_USERNAME:
                return resolveUsername(value);
            case ViewerConstants.POST_PROCESSOR_TO_UPPER_CASE:
                return toUpperCase(value);
            }

        }
        return value;
    }

    private static String resolveUsername(String value) {
        UserNameResolverHelper unr = new UserNameResolverHelper();
        return unr.getUserFullName(value);
    }

    private static String toUpperCase(String value) {
        if (!value.isEmpty()) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value;
    }

}
