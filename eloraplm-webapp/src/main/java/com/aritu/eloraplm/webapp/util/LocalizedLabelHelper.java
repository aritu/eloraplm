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
package com.aritu.eloraplm.webapp.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

import com.google.common.collect.TreeMultimap;

/**
 *
 * @author aritu
 *
 */
public class LocalizedLabelHelper {

    private static Log log = LogFactory.getLog(LocalizedLabelHelper.class);

    private static final String SUPPORTED_LOCALES_VOCABULARY = "locale";

    private static final String LABEL_MAP_KEY_LOCALE = "locale";

    private static final String LABEL_MAP_KEY_LABEL = "label";

    private static final List<String> supportedLocales = initSupportedLocales();

    private static final String defaultLocale = initDefaultLocale();

    private static List<String> initSupportedLocales() {
        TreeMultimap<Integer, String> supportedLocalesMap = TreeMultimap.create();

        new UnrestrictedSessionRunner("default") {

            @Override
            public void run() {

                DirectoryService directoryService = Framework.getLocalService(
                        DirectoryService.class);
                Session dirSession = null;

                try {
                    dirSession = directoryService.open(
                            SUPPORTED_LOCALES_VOCABULARY);

                    DocumentModelList vocabDocs = dirSession.query(
                            new HashMap<String, Serializable>());
                    for (DocumentModel vocabDoc : vocabDocs) {
                        if (vocabDoc != null) {
                            String locale = vocabDoc.getId();
                            int order = (int) (long) vocabDoc.getPropertyValue(
                                    "vocabulary:ordering");
                            // Even if we support different xx_YY locales for
                            // the same xx language, we remove duplicates here.
                            String localeIso639 = locale.substring(0, 2);
                            if (!supportedLocalesMap.containsValue(
                                    localeIso639)) {
                                supportedLocalesMap.put(order, localeIso639);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("[initSupportedLocales] Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                            e);
                } finally {
                    if (dirSession != null) {
                        dirSession.close();
                    }
                }
            }

        }.runUnrestricted();
        return new ArrayList<String>(supportedLocalesMap.values());

    }

    private static String initDefaultLocale() {
        Locale locale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        // Even if default locale can be xx_YY, we just need the xx language
        // part.
        return locale != null ? locale.getLanguage().substring(0, 2) : "en";
    }

    public static List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public static List<LocalizedLabel> initializeLabelList(
            List<LocalizedLabel> labels) {

        labels = new ArrayList<LocalizedLabel>();

        for (String locale : supportedLocales) {
            labels.add(new LocalizedLabel(locale, null));
        }

        return labels;
    }

    public static List<LocalizedLabel> convertMapListToObjectList(
            List<Map<String, String>> labelMaps) {
        List<LocalizedLabel> labels = new ArrayList<LocalizedLabel>();

        for (Map<String, String> labelMap : labelMaps) {
            if (labelMap.containsKey(LABEL_MAP_KEY_LOCALE)
                    && labelMap.containsKey(LABEL_MAP_KEY_LABEL)) {
                labels.add(
                        new LocalizedLabel(labelMap.get(LABEL_MAP_KEY_LOCALE),
                                labelMap.get(LABEL_MAP_KEY_LABEL)));
            }
        }
        return labels;
    }

    public static List<Map<String, String>> convertObjectListToMapList(
            List<LocalizedLabel> labels) {
        List<Map<String, String>> labelMaps = new ArrayList<Map<String, String>>();

        for (LocalizedLabel label : labels) {
            Map<String, String> labelMap = new HashMap<String, String>();
            labelMap.put(LABEL_MAP_KEY_LOCALE, label.getLocale());
            labelMap.put(LABEL_MAP_KEY_LABEL, label.getLabel());
            labelMaps.add(labelMap);
        }
        return labelMaps;
    }

    public static String getLocalizedLabel(List<LocalizedLabel> labels,
            String userLocale, String fallback) {

        String defaultLocale = getDefaultLocale();

        String value = null;
        String defaultValue = null;
        if (labels != null) {
            for (LocalizedLabel label : labels) {
                if (userLocale != null && label.getLocale().equals(userLocale)
                        && label.getLabel() != null) {
                    value = label.getLabel();
                    break;
                }
                if (label.getLocale().equals(defaultLocale)) {
                    defaultValue = label.getLabel();
                }
            }
        }

        if (value == null) {
            value = defaultValue == null ? fallback : defaultValue;
        }

        return value;
    }

    public static String getDefaultLocale() {
        return defaultLocale;
    }

}
