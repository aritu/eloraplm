/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.workflows.util;

import java.util.Locale;

import javax.faces.context.FacesContext;

import org.elasticsearch.common.lang3.StringUtils;

import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.google.common.base.CaseFormat;

/**
 *
 * @author aritu
 *
 */
public class TaskStatusHelper {

    private static final String[] TASK_STATUS_OK = { "validate", "verify",
            "accept", "approve", "ok", "homologate" };

    private static final String[] TASK_STATUS_KO = { "reject", "cancel", "deny",
            "refuse", "ko" };

    public static String convertStatus(FacesContext context, Object value,
            boolean addStyle) {

        Locale locale = context.getViewRoot().getLocale();
        return convertStatus(locale.toString(), (String) value, addStyle);
    }

    public static String convertStatus(String locale, String initialValue,
            boolean addStyle) {

        // Remove ordering number and ending _x (to exec immediately without
        // evaluation)
        String convertedValue = StringUtils.removeEnd(
                initialValue.replaceFirst("(\\d+)_", "").replace("_cfm", ""),
                "_x");
        String translatedValue = convertedValue.isEmpty() ? ""
                : EloraMessageHelper.getTranslatedMessage(locale,
                        "eloraplm.wf.btn." + CaseFormat.LOWER_UNDERSCORE.to(
                                CaseFormat.LOWER_CAMEL, convertedValue));

        if (addStyle) {
            if (StringUtils.startsWithAny(convertedValue, TASK_STATUS_OK)) {
                convertedValue = "<span class=\"taskStatusOk\">"
                        + translatedValue + "</span>";
            } else if (StringUtils.startsWithAny(convertedValue,
                    TASK_STATUS_KO)) {
                convertedValue = "<span class=\"taskStatusKo\">"
                        + translatedValue + "</span>";
            } else {
                convertedValue = "<span class=\"taskStatusNormal\">"
                        + translatedValue + "</span>";
            }
        } else {
            convertedValue = translatedValue;
        }

        return convertedValue;
    }
}
