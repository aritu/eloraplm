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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;
import com.aritu.eloraplm.webapp.util.LocalizedLabelHelper;

/**
 * @author aritu
 *
 */
@Name("localizedLabelActions")
@Scope(SESSION)
public class LocalizedLabelActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            LocalizedLabelActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private String defaultLocale;

    public String getLocalizedLabelFromMap(List<Map<String, String>> labelMaps,
            String userLocale, String fallback) {

        List<LocalizedLabel> labels = LocalizedLabelHelper.convertMapListToObjectList(
                labelMaps);

        return getLocalizedLabel(labels, userLocale, fallback);
    }

    public String getLocalizedLabel(List<LocalizedLabel> labels,
            String userLocale, String fallback) {

        return LocalizedLabelHelper.getLocalizedLabel(labels, userLocale,
                fallback);
    }

    public List<LocalizedLabel> initializeLabelList(
            List<LocalizedLabel> labels) {
        return LocalizedLabelHelper.initializeLabelList(labels);
    }

    public String getDefaultLocale() {
        if (defaultLocale == null) {
            defaultLocale = LocalizedLabelHelper.getDefaultLocale();
        }
        return defaultLocale;
    }

    public List<String> getSupportedLocales() {
        return LocalizedLabelHelper.getSupportedLocales();
    }
}
