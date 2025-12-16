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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.admin.runtime.RuntimeInstrospection;
import org.nuxeo.ecm.admin.runtime.SimplifiedBundleInfo;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.types.api.TypeConfigService;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Name("eloraWebActions")
@Scope(CONVERSATION)
public class EloraWebActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EloraWebActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected Map<String, String> messages;

    private Boolean isAny2PdfConverterAvailable;

    /**
     * Get bean instance by name
     * 
     * @param name
     * @return
     */
    public static Object getBeanByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Component.getInstance(name);
    }

    /**
     * @param directoryName
     * @param entryId
     * @return
     */
    public static DocumentModel getDirectoryEntry(String directoryName,
            String entryId) {
        if (entryId == null) {
            return null;
        }
        DirectoryService dirService = Framework.getService(
                DirectoryService.class);
        try (Session session = dirService.open(directoryName)) {
            return session.getEntry(entryId);
        }
    }

    /**
     * @return
     */
    public boolean isInAWorkspace() {

        DocumentModel superSpace = documentManager.getSuperSpace(
                navigationContext.getCurrentDocument());

        if (superSpace.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            return true;
        }

        return false;
    }

    public boolean existsDocument(String id) {
        return id != null && !id.isEmpty()
                ? documentManager.exists(new IdRef(id))
                : false;
    }

    public DocumentRef getDocumentRefFromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        DocumentRef docRef = new IdRef(id);

        return docRef;
    }

    public DocumentModel getDocumentFromId(String id) {
        DocumentRef docRef = getDocumentRefFromId(id);
        if (docRef != null) {
            return getDocument(docRef);
        }

        return null;
    }

    public DocumentModel getDocument(DocumentRef docRef) {
        String logInitMsg = "[getDocument] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (docRef == null) {
            return null;
        }
        DocumentModel docM = null;
        try {

            docM = documentManager.getDocument(docRef);

        } catch (DocumentNotFoundException | DocumentSecurityException e) {
            log.error(logInitMsg + "Document with docRef = |" + docRef
                    + "| cannot be retrieved. It does not exist any more or the user has no permission to read it. Exception message = |"
                    + e.getMessage() + "|");

            return null;
        }

        return docM;
    }

    public List<DocumentRef> getDocumentVersionRefs(DocumentRef docRef) {
        List<DocumentRef> versionRefs = new ArrayList<DocumentRef>();

        if (docRef != null) {
            versionRefs = documentManager.getVersionsRefs(docRef);
        }

        return versionRefs;
    }

    public boolean isBaseVersion(DocumentRef avRef) {
        DocumentModel wcDoc = documentManager.getWorkingCopy(avRef);
        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
        if (baseDoc != null && baseDoc.getId().equals(avRef.toString())) {
            return true;
        }

        return false;
    }

    public boolean isAvObsolete() {
        try {
            return EloraDocumentHelper.isAvObsolete(
                    navigationContext.getCurrentDocument());
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.checkIfDocIsObsolete"));
            return false;
        }
    }

    public boolean isWcObsolete() {
        try {
            return EloraDocumentHelper.isWcObsolete(
                    navigationContext.getCurrentDocument());
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.checkIfDocIsObsolete"));
            return false;
        }
    }

    /**
     * @param doc
     * @return
     */
    public boolean isEditable() {
        return EloraDocumentHelper.isEditable(
                navigationContext.getCurrentDocument());
    }

    public boolean hasBaseVersion() {
        if (EloraDocumentHelper.getBaseVersion(
                navigationContext.getCurrentDocument()) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param expr
     * @return
     */
    public Boolean evaluateBoolean(String expr) {
        FacesContext context = FacesContext.getCurrentInstance();
        Boolean result = context.getApplication().evaluateExpressionGet(context,
                expr, Boolean.class);

        return result;
    }

    /**
     * @param expr
     * @return
     */
    public String evaluateString(String expr) {

        FacesContext context = FacesContext.getCurrentInstance();
        String result = context.getApplication().evaluateExpressionGet(context,
                expr, String.class);

        return result;
    }

    /**
     * @param expr
     * @return
     */
    public Integer evaluateInteger(String expr) {

        FacesContext context = FacesContext.getCurrentInstance();
        Integer result = context.getApplication().evaluateExpressionGet(context,
                expr, Integer.class);

        return result;
    }

    public Object evaluateObject(String expr) {

        FacesContext context = FacesContext.getCurrentInstance();
        Object result = context.getApplication().evaluateExpressionGet(context,
                expr, Object.class);

        return result;
    }

    public GregorianCalendar stringToCalendar(String value, String pattern) {
        String logInitMsg = "[stringToCalendar] ["
                + documentManager.getPrincipal().getName() + "] ";

        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        try {
            DateFormat df = new SimpleDateFormat(pattern);
            Date date = df.parse(value);
            cal.setTime(date);
        } catch (ParseException e) {
            log.error(logInitMsg + "Error parsing string |" + value
                    + "| to calendar: " + e.getMessage(), e);
        }

        return cal;
    }

    public String getEloraDownloadLink() throws EloraException {

        String logInitMsg = "[getEloraDownloadLink] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }
        if (!currentDoc.isImmutable()) {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            if (baseDoc == null) {
                log.error(logInitMsg + "The document |" + currentDoc.getId()
                        + "| has no base version. Probably because it has no AVs.");
                // We cannot show a facesMessage because this method is not
                // called when the user presses the button, it is called when
                // the summary page is displayed, so the error message is
                // displayed in the next action

                // If no baseDoc exists (or we are not able to get it, for
                // example from the plugin) the link is empty
                return "";
            }
            currentDoc = baseDoc;
        }

        return "elora://" + documentManager.getPrincipal().getName() + "/"
                + currentDoc.getId() + "/"
                + currentDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL)
                + "/" + currentDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION);
    }

    public void registerDownloadAndOpenAction() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }
        if (!currentDoc.isImmutable()) {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            if (baseVersion != null) {
                currentDoc = baseVersion;
            }
        }

        // Nuxeo Event
        String comment = currentDoc.getVersionLabel();
        EloraEventHelper.fireEvent(
                EloraEventNames.ELORA_DOWNLOAD_AND_OPEN_EVENT, currentDoc,
                comment);
    }

    public Boolean getIsAny2PdfConverterAvailable() {
        if (isAny2PdfConverterAvailable == null) {
            isAny2PdfConverterAvailable = Boolean.valueOf(
                    checkIfAny2PdfConverterIsAvailable());
        }

        return isAny2PdfConverterAvailable;

    }

    private boolean checkIfAny2PdfConverterIsAvailable() {
        ConversionService cs = Framework.getService(ConversionService.class);
        try {
            cs.isConverterAvailable("any2pdf");
            return true;
        } catch (ConverterNotRegistered e) {
            return false;
        }

    }

    public String getEloraPlmVersion() {
        String version = null;

        SimplifiedServerInfo ssi = RuntimeInstrospection.getInfo();
        List<SimplifiedBundleInfo> bundles = ssi.getBundleInfos();
        for (SimplifiedBundleInfo bundle : bundles) {
            // We use core bundle as reference
            if (bundle.getName().equals("com.aritu.eloraplm.core")) {
                version = bundle.getVersion();
            }
        }
        return version;
    }

    public Map<Object, Object> getEloraPlmProperties() {
        Properties props = Framework.getProperties();
        Map<Object, Object> eloraProps = new TreeMap<Object, Object>();
        props.forEach((key, value) -> {
            if (key.toString().startsWith("com.aritu.eloraplm.")) {
                eloraProps.put(key, value);
            }
        });

        return eloraProps;
    }

    public List<SortInfo> createSortInfoList(String column, boolean ascending) {
        List<SortInfo> sil = new ArrayList<SortInfo>();
        SortInfo si = new SortInfo(column, ascending);
        sil.add(si);
        return sil;
    }

    public boolean isArchived() {
        return EloraDocumentHelper.isArchived(
                navigationContext.getCurrentDocument());
    }

    public boolean userIsInUserGroupList(String user,
            List<String> userGroupList) {

        for (String userGroup : userGroupList) {
            if (user.equals(userGroup)) {
                return true;
            }

            if (Functions.userIsMemberOf(userGroup)) {
                return true;
            }
        }

        return false;

    }

    public Date getDateFromString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant i = Instant.from(dtf.parse(dateStr));
        return Date.from(i);
    }

    public String formatDateWithPattern(Date date, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.format(date);
    }

    public Map<String, Object> reverseLinkedHashMap(
            LinkedHashMap<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        List<String> keys = new ArrayList<String>(source.keySet());
        Collections.reverse(keys);
        for (String key : keys) {
            result.put(key, source.get(key));
        }
        return result;
    }

    public List<Object> reverseList(List<Object> list) {
        Collections.reverse(list);
        return list;
    }

    public String getColorForType(String type) {
        TypeConfigService tcs = Framework.getService(TypeConfigService.class);
        if (tcs == null) {
            return TypeConfigService.TYPE_DEFAULT_COLOR;
        } else {
            return tcs.getColorForType(type);
        }
    }

    public boolean startsWith(String text, String prefix) {
        return text.startsWith(prefix);
    }

    public boolean existsWidget(String category, String name) {
        LayoutStore ls = Framework.getService(LayoutStore.class);
        if (ls != null) {
            WidgetDefinition wd = ls.getWidgetDefinition(category, name);
            if (wd != null) {
                return true;
            }
        }

        return false;
    }

    public List<Integer> getNumberListForLoop(int size) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 1; i <= size; i++) {
            list.add(i);
        }
        return list;
    }

    public String getRestDocumentLink(DocumentModel doc, String viewId, Map<String, String> params,
            boolean useNewConversation) {
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        /*
         * return DocumentModelFunctions.documentUrl(null, doc, viewId, params,
         * useNewConversation);
         */
        return DocumentModelFunctions.documentUrl(null, docLoc, viewId, params,
                useNewConversation,
                BaseURL.getBaseURL());

    }

    public Map<String, String> getTreeExportParams(String widgetName, String treeBeanName, String format) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("widgetName", widgetName);
        map.put("treeBeanName", treeBeanName);
        map.put("format", format);
        return map;
    }

    public Map<String, String> getTableExportParams(String widgetName, String tableBeanName, String format) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("widgetName", widgetName);
        map.put("tableBeanName", tableBeanName);
        map.put("format", format);
        return map;
    }
}
