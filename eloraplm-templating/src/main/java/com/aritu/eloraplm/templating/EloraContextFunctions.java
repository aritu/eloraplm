package com.aritu.eloraplm.templating;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.template.api.context.DocumentWrapper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.templating.util.CharacteristicInfo;
import com.aritu.eloraplm.templating.util.HistoryVersionInfo;
import com.aritu.eloraplm.templating.util.RelationInfo;

public class EloraContextFunctions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    protected final DocumentModel currentDocWithFixedCommonData;

    protected UserNameResolverHelper unr;

    public EloraContextFunctions(DocumentModel doc,
            DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
        currentDocWithFixedCommonData = createCurrentDocWithFixedCommonData();

        unr = new UserNameResolverHelper();
    }

    private DocumentModel createCurrentDocWithFixedCommonData() {
        if (doc.isImmutable()) {
            return doc;
        } else {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(doc);
            return baseVersion == null ? doc : baseVersion;
        }
    }

    public DocumentModel getCurrentDocWithFixedCommonData() {
        return currentDocWithFixedCommonData;
    }

    public String getFixedDateMetadata(String metadata) {
        Serializable propValue = currentDocWithFixedCommonData.getPropertyValue(
                metadata);
        if (propValue != null) {
            GregorianCalendar cal = (GregorianCalendar) propValue;
            return formatInternationalDateTime(cal);
        }

        return "";
    }

    public String getFixedMetadata(String metadata) {
        Serializable propValue = currentDocWithFixedCommonData.getPropertyValue(
                metadata);
        if (propValue != null) {
            return propValue.toString();
        }

        return "";
    }

    public String getFixedUserMetadata(String metadata) {
        Serializable propValue = currentDocWithFixedCommonData.getPropertyValue(
                metadata);
        if (propValue != null) {
            return unr.getUserFullName(propValue.toString());
        }

        return "";
    }

    public String getDisplayNameForUsername(String username) {
        String displayName = username;
        NuxeoPrincipal principal = new NuxeoPrincipalImpl(username);
        if (principal != null) {
            displayName = principal.getFirstName() + " "
                    + principal.getLastName();
        }
        return displayName;
    }

    public List<HistoryVersionInfo> getVersioningHistory(int limit)
            throws EloraException {

        List<HistoryVersionInfo> versionInfoList = new ArrayList<HistoryVersionInfo>();

        if (limit > 0) {
            // First, include current version at first position
            versionInfoList.add(
                    createVersionInfoFromDoc(currentDocWithFixedCommonData));

            // Then, retrieve (limit -1) older released and obsolete versions
            if (limit > 1) {
                CoreSession session = doc.getCoreSession();

                long majorVersion = (long) doc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

                DocumentModelList versions = EloraDocumentHelper.getOlderReleasedOrObsoleteVersions(
                        session, doc.getRef(), majorVersion, limit - 1);

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

    private HistoryVersionInfo createVersionInfoFromDoc(DocumentModel ver) {

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

        List<String> predicateList = getEbomPredicateUris();
        long count = EloraQueryFactory.countObjectRelationsForDocumentByPredicateList(
                doc.getCoreSession(), doc.getId(), predicateList);
        if (count > 0L) {
            return true;
        }
        return false;
    }

    public List<RelationInfo> getEbom() {
        CoreSession session = doc.getCoreSession();

        List<RelationInfo> relations = new ArrayList<RelationInfo>();

        List<Resource> predicatesList = getEbomPredicates();

        List<Statement> relatedStmts;
        relatedStmts = EloraRelationHelper.getStatements(doc, predicatesList);

        if (!relatedStmts.isEmpty()) {

            for (Statement stmt : relatedStmts) {
                DocumentModel relatedDoc = null;

                relatedDoc = RelationHelper.getDocumentModel(stmt.getObject(),
                        session);
                if (relatedDoc != null) {
                    EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                            stmt);
                    RelationInfo relInfo = new RelationInfo();
                    relInfo.setUid(relatedDoc.getId());
                    relInfo.setReference(relatedDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString());
                    relInfo.setTitle(relatedDoc.getTitle());
                    relInfo.setVersionLabel(relatedDoc.getVersionLabel());
                    relInfo.setLifecycleState(
                            relatedDoc.getCurrentLifeCycleState());
                    relInfo.setQuantity(stmtInfo.getQuantity());
                    if (stmtInfo.getOrdering() != null) {
                        relInfo.setOrdering(
                                String.valueOf(stmtInfo.getOrdering()));
                    } else {
                        relInfo.setOrdering("");
                    }

                    relations.add(relInfo);
                }
            }

        }

        return relations;
    }

    private List<String> getEbomPredicateUris() {
        List<String> predicateUris = new ArrayList<String>();
        predicateUris.addAll(RelationsConfig.bomHierarchicalRelationsList);
        predicateUris.addAll(RelationsConfig.bomDirectRelationsList);

        return predicateUris;
    }

    private List<Resource> getEbomPredicates() {
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
                doc.getCoreSession(), doc.getType(), doc.getId());
        if (count > 0L) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<CharacteristicInfo> getCharacteristics() {

        List<CharacteristicInfo> characteristics = new ArrayList<CharacteristicInfo>();

        ArrayList<HashMap<String, Object>> characteristicsContent = new ArrayList<HashMap<String, Object>>();
        if (doc.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
            characteristicsContent = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
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
                                ? "Yes" : "No";
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

}
