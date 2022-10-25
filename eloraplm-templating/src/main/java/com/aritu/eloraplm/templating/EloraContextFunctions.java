package com.aritu.eloraplm.templating;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.template.api.context.DocumentWrapper;

import com.aritu.eloraplm.bom.util.BomHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.templating.util.CharacteristicInfo;
import com.aritu.eloraplm.templating.util.HistoryVersionInfo;
import com.aritu.eloraplm.templating.util.RelationInfo;

public class EloraContextFunctions {

    private static final Log log = LogFactory.getLog(
            EloraContextFunctions.class);

    /**
     * This variable contains the list of metadata which value has to be
     * retrieved from the base version of the document. For example, when
     * overwriting a document, last modified date is not the current document
     * last modified date, but the last modified date of the base version.
     *
     */
    protected final List<String> baseVersionBasedMetadataList = new ArrayList<String>(
            Arrays.asList(
                    // dates
                    NuxeoMetadataConstants.NX_DC_CREATED,
                    NuxeoMetadataConstants.NX_DC_MODIFIED,
                    EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE,
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED,
                    // users
                    NuxeoMetadataConstants.NX_DC_CREATOR,
                    NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR,
                    EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY,
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER));

    // current document
    protected final DocumentModel currentDoc;

    // base version of the current document
    protected final DocumentModel currentDocBaseVersion;

    protected final DocumentWrapper nuxeoWrapper;

    protected UserNameResolverHelper unr;

    protected List<Statement> eBomrelatedStmts;

    public EloraContextFunctions(DocumentModel doc,
            DocumentWrapper nuxeoWrapper) {
        currentDoc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
        currentDocBaseVersion = createCurrentDocBaseVersion();

        unr = new UserNameResolverHelper();

        // It will be initialized first time is used
        eBomrelatedStmts = null;
    }

    private DocumentModel createCurrentDocBaseVersion() {
        if (currentDoc.isImmutable()) {
            return currentDoc;
        } else {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            return baseVersion == null ? currentDoc : baseVersion;
        }
    }

    public DocumentModel getDocFromId(String docId) {
        return currentDoc.getCoreSession().getDocument(new IdRef(docId));
    }

    public String getCurrentDocMetadata(String metadata) {
        String logInitMsg = "[getCurrentDocMetadata] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String propValue = null;
            if (baseVersionBasedMetadataList.contains(metadata)) {
                propValue = getMetadata(currentDocBaseVersion, metadata);
            } else {
                propValue = getMetadata(currentDoc, metadata);
            }
            return propValue;
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getMetadata(DocumentModel doc, String metadata) {
        Serializable propValue = doc.getPropertyValue(metadata);
        if (propValue != null) {
            return propValue.toString();
        }
        return "";
    }

    public String getCurrentDocDateMetadata(String metadata) {
        String logInitMsg = "[getCurrentDocDateMetadata] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String propValue = null;
            if (baseVersionBasedMetadataList.contains(metadata)) {
                propValue = getDateMetadata(currentDocBaseVersion, metadata);
            } else {
                propValue = getDateMetadata(currentDoc, metadata);
            }
            return propValue;
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getDateMetadata(DocumentModel doc, String metadata) {
        Serializable propValue = doc.getPropertyValue(metadata);
        if (propValue != null) {
            GregorianCalendar cal = (GregorianCalendar) propValue;
            return formatInternationalDateTime(cal);
        }

        return "";
    }

    public String getCurrentDocUserMetadata(String metadata) {
        String logInitMsg = "[getCurrentDocUserMetadata] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String propValue = null;
            if (baseVersionBasedMetadataList.contains(metadata)) {
                propValue = getUserMetadata(currentDocBaseVersion, metadata);
            } else {
                propValue = getUserMetadata(currentDoc, metadata);
            }
            return propValue;
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getUserMetadata(DocumentModel doc, String metadata) {
        Serializable propValue = doc.getPropertyValue(metadata);
        if (propValue != null) {
            return getDisplayNameForUsername(propValue.toString());
        }
        return "";
    }

    public String getCurrentDocVersionLabel() {
        String logInitMsg = "[getCurrentDocVersionLabel] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String versionLabel = currentDocBaseVersion.getVersionLabel();
            if (versionLabel != null) {
                return versionLabel;
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getExternalVersionOrCurrentDocVersionLabel() {
        String logInitMsg = "[getExternalVersionOrCurrentDocVersionLabel] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String versionLabel = null;
            if (currentDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION) != null
                    && !currentDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION).toString().isEmpty()) {
                versionLabel = (String) currentDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION);
            } else {
                versionLabel = currentDocBaseVersion.getVersionLabel();
            }

            return versionLabel;
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getCurrentDocLifeCycleState() {
        String logInitMsg = "[getCurrentDocLifeCycleState] ["
                + currentDoc.getCoreSession().getPrincipal().getName() + "] ";
        try {
            String lifeCycleState = currentDocBaseVersion.getCurrentLifeCycleState();
            if (lifeCycleState != null) {
                return lifeCycleState;
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        return "";
    }

    public String getDisplayNameForUsername(String username) {
        String displayName = username;
        NuxeoPrincipal principal = new NuxeoPrincipalImpl(username);
        if (principal != null) {
            if (principal.getFirstName() != null
                    && principal.getLastName() != null) {
                displayName = principal.getFirstName() + " "
                        + principal.getLastName();
            } else {
                displayName = unr.getUserFullName(username);
            }
        }
        return displayName;
    }

    public List<HistoryVersionInfo> getVersioningHistory(int limit)
            throws EloraException {

        List<HistoryVersionInfo> versionInfoList = new ArrayList<HistoryVersionInfo>();

        if (limit > 0) {
            // First, include current version at first position
            versionInfoList.add(
                    createVersionInfoFromDoc(currentDocBaseVersion));

            // Then, retrieve (limit -1) older released and obsolete versions
            if (limit > 1) {
                CoreSession session = currentDoc.getCoreSession();

                long majorVersion = (long) currentDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

                DocumentModelList versions = EloraDocumentHelper.getOlderReleasedOrObsoleteVersions(
                        session, currentDoc.getRef(), majorVersion, limit - 1);

                if (versions != null && versions.size() > 0) {
                    for (DocumentModel ver : versions) {
                        versionInfoList.add(createVersionInfoFromDoc(ver));
                    }
                }
            }
        }
        // else, nothing to do.

        return versionInfoList;
    }

    protected HistoryVersionInfo createVersionInfoFromDoc(DocumentModel ver) {

        HistoryVersionInfo versionInfo = new HistoryVersionInfo();
        versionInfo.setUid(ver.getId());
        Serializable reference = ver.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        if (reference != null) {
            versionInfo.setReference(reference.toString());
        }
        versionInfo.setVersionLabel(ver.getVersionLabel());
        String comment = ver.getCheckinComment() == null ? ""
                : ver.getCheckinComment();
        versionInfo.setComment(comment);
        versionInfo.setLifecycleState(ver.getCurrentLifeCycleState());
        versionInfo.setLastContributor(unr.getUserFullName(ver.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR).toString()));
        GregorianCalendar modifiedCalendar = (GregorianCalendar) ver.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        versionInfo.setModified(formatInternationalDateTime(modifiedCalendar));

        return versionInfo;
    }

    public String formatInternationalDateTime(GregorianCalendar calendar) {
        // We have to clone the calendar, so we do not alter its format
        GregorianCalendar calendarForFormatting = (GregorianCalendar) calendar.clone();

        calendarForFormatting.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fmt.setCalendar(calendarForFormatting);
        return fmt.format(calendarForFormatting.getTime()) + " UTC";
    }

    public boolean hasEbom() {
        boolean hasEbom = false;
        // If it is not initialized yet, initialize it
        if (eBomrelatedStmts == null) {
            initEBomRelatedStatements();
        }
        if (!eBomrelatedStmts.isEmpty()) {
            hasEbom = true;
        }
        return hasEbom;
    }

    protected void initEBomRelatedStatements() {
        List<Resource> predicatesList = getEbomPredicates();
        eBomrelatedStmts = EloraRelationHelper.getStatements(currentDoc,
                predicatesList);
    }

    public List<RelationInfo> getEbom() {
        return getEbom(false, false, null);
    }

    public List<RelationInfo> getEbom(String classification) {
        return getEbom(false, false, classification);
    }

    public List<RelationInfo> getEbomWithExternalVersions() {
        return getEbom(true, true, null);
    }

    public List<RelationInfo> getEbomWithExternalVersions(
            String classification) {
        return getEbom(true, true, classification);
    }

    public List<RelationInfo> getEbomWithExternalVersionsNoFallback() {
        return getEbom(true, false, null);
    }

    public List<RelationInfo> getEbomWithExternalVersionsNoFallback(
            String classification) {
        return getEbom(true, false, classification);
    }

    /**
     * If given classification is not null, returns the eBOM related documents
     * having the given classification value.
     *
     * @param classification if not null, related eBom documents will be
     *            filtered by the given classification value
     * @param showExternalVersions
     * @return
     */
    private List<RelationInfo> getEbom(boolean showExternalVersions,
            boolean fallbackToEloraVersion, String classification) {
        CoreSession session = currentDoc.getCoreSession();

        List<RelationInfo> relations = new ArrayList<RelationInfo>();

        // If it is not initialized yet, initialize it
        if (eBomrelatedStmts == null) {
            initEBomRelatedStatements();
        }
        if (!eBomrelatedStmts.isEmpty()) {
            for (Statement stmt : eBomrelatedStmts) {
                DocumentModel relatedDoc = null;

                relatedDoc = RelationHelper.getDocumentModel(stmt.getObject(),
                        session);
                if (relatedDoc != null) {
                    boolean filterClasificationResult = false;
                    if (classification != null) {
                        String relatedDocClassification = BomHelper.getBomClassificationValue(
                                relatedDoc);
                        if (relatedDocClassification != null
                                && relatedDocClassification.equals(
                                        classification)) {
                            filterClasificationResult = true;
                        }
                    }

                    if (classification == null
                            || filterClasificationResult == true) {
                        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                                stmt);
                        RelationInfo relInfo = createRelationInfoFromDoc(
                                relatedDoc, stmtInfo, showExternalVersions,
                                fallbackToEloraVersion);
                        relations.add(relInfo);
                    }
                }
            }
        }
        return relations;
    }

    protected RelationInfo createRelationInfoFromDoc(DocumentModel relatedDoc,
            EloraStatementInfo stmtInfo, boolean showExternalVersions,
            boolean fallbackToEloraVersion) {
        RelationInfo relInfo = new RelationInfo();
        relInfo.setUid(relatedDoc.getId());
        relInfo.setReference((String) relatedDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE));
        relInfo.setTitle(relatedDoc.getTitle());

        String versionLabel = null;
        String externalVersionLabel = null;
        if (relatedDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION) != null
                && !relatedDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION).toString().isEmpty()) {
            externalVersionLabel = (String) relatedDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION);
        }

        if (showExternalVersions && externalVersionLabel != null) {
            versionLabel = externalVersionLabel;
        } else if (!showExternalVersions || fallbackToEloraVersion) {
            versionLabel = relatedDoc.getVersionLabel();
        }
        relInfo.setVersionLabel(versionLabel);

        String checksum = "";
        if (relatedDoc.getType().equals(EloraDoctypeConstants.SOFTWARE)
                && relatedDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_SOFTWARE_CHECKSUM) != null
                && !relatedDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_SOFTWARE_CHECKSUM).toString().isEmpty()) {
            checksum = (String) relatedDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_SOFTWARE_CHECKSUM);
        }
        relInfo.setChecksum(checksum);

        relInfo.setLifecycleState(relatedDoc.getCurrentLifeCycleState());
        relInfo.setQuantity(stmtInfo.getQuantity());
        if (stmtInfo.getOrdering() != null) {
            relInfo.setOrdering(String.valueOf(stmtInfo.getOrdering()));
        } else {
            relInfo.setOrdering("");
        }
        return relInfo;
    }

    private List<String> getEbomPredicateUris() {
        List<String> predicateUris = new ArrayList<String>();
        predicateUris.addAll(RelationsConfig.bomHierarchicalRelationsList);
        predicateUris.addAll(RelationsConfig.bomDirectRelationsList);

        return predicateUris;
    }

    protected List<Resource> getEbomPredicates() {
        List<String> predicatesUris = getEbomPredicateUris();

        return loadPredicateResources(predicatesUris);
    }

    private List<Resource> loadPredicateResources(List<String> predicatesList) {
        List<Resource> predicates = new ArrayList<Resource>();
        for (String predicateUri : predicatesList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            predicates.add(predicateResource);
        }

        return predicates;
    }

    class SortCharacteristicsByOrder implements Comparator<CharacteristicInfo> {
        @Override
        public int compare(CharacteristicInfo a, CharacteristicInfo b) {
            return a.getOrder() - b.getOrder();
        }
    }

    public boolean hasCharacteristics() {

        long count = EloraQueryFactory.countCharacteristicsShownInReport(
                currentDoc.getCoreSession(), currentDoc.getType(),
                currentDoc.getId());
        if (count > 0L) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<CharacteristicInfo> getCharacteristics() {

        List<CharacteristicInfo> characteristics = new ArrayList<CharacteristicInfo>();

        ArrayList<HashMap<String, Object>> characteristicsContent = new ArrayList<HashMap<String, Object>>();
        if (currentDoc.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
            characteristicsContent = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);

            for (HashMap<String, Object> characteristicContent : characteristicsContent) {
                boolean showInReport = false;
                if (characteristicContent.containsKey("showInReport")) {
                    showInReport = Boolean.parseBoolean(
                            characteristicContent.get(
                                    "showInReport").toString());
                }

                if (showInReport) {
                    String title = characteristicContent.containsKey("title")
                            && characteristicContent.get("title") != null
                                    ? characteristicContent.get(
                                            "title").toString()
                                    : "";
                    String value = getCharacteristicValue(
                            characteristicContent);
                    String unit = characteristicContent.containsKey("unit")
                            && characteristicContent.get("unit") != null
                                    ? characteristicContent.get(
                                            "unit").toString()
                                    : "";

                    CharacteristicInfo characteristic = new CharacteristicInfo(
                            title, value, unit);

                    Object orderObj = characteristicContent.get(
                            "orderInReport");
                    if (orderObj != null) {
                        Integer order = (int) (long) orderObj;
                        characteristic.setOrder(order);
                    } else {
                        // We put 1000 as order so we ensure it is the last
                        characteristic.setOrder(1000);
                    }

                    characteristics.add(characteristic);
                }
            }

            // Order the list
            Collections.sort(characteristics, new SortCharacteristicsByOrder());
        }

        return characteristics;
    }

    private String getCharacteristicValue(
            HashMap<String, Object> characteristic) {

        String value = "";

        if (characteristic.containsKey("type")
                && characteristic.get("type") != null) {

            switch (characteristic.get("type").toString()) {
            case "number":
                value = returnStringValueIfNotNull(
                        characteristic.get("numberValue"));
                break;
            case "string":
                value = returnStringValueIfNotNull(
                        characteristic.get("stringValue"));
                break;
            case "date":
                if (characteristic.get("dateValue") != null) {
                    GregorianCalendar dateValue = (GregorianCalendar) characteristic.get(
                            "dateValue");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    value = df.format(dateValue.getTime());
                }
                break;
            case "boolean":
                value = returnStringValueIfNotNull(
                        characteristic.get("booleanValue")).equals("true")
                                ? "Yes"
                                : "No";
                break;
            case "list":
                value = returnStringValueIfNotNull(
                        characteristic.get("listValue"));
                break;
            }
        }

        return value;
    }

    private String returnStringValueIfNotNull(Object property) {
        if (property != null) {
            return property.toString();
        } else {
            return "";
        }
    }

    public String getLocalizedMessage(String key) {

        return EloraMessageHelper.getTranslatedMessage(
                currentDoc.getCoreSession(), key);
    }

    public String getLocalizedEuMessage(String key) {

        return EloraMessageHelper.getTranslatedEuMessage(key);
    }

    public String getLocalizedEsMessage(String key) {

        return EloraMessageHelper.getTranslatedEsMessage(key);
    }

    public String getLocalizedEnMessage(String key) {

        return EloraMessageHelper.getTranslatedEnMessage(key);
    }

}
