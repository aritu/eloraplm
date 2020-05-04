package com.aritu.eloraplm.versioning;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.versioning.StandardVersioningService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;

public class EloraVersioningService extends StandardVersioningService {

    @Override
    public String getVersionLabel(DocumentModel docModel) {
        String label;
        try {
            label = getMajorToDisplay(docModel) + "."
                    + getMinorToDisplay(docModel);
            if (docModel.isCheckedOut()) {
                label += "+";
            }
        } catch (PropertyNotFoundException e) {
            label = "";
        }
        return label;
    }

    protected String getMajorToDisplay(DocumentModel docModel) {
        EloraVersionLabelService versionLabelService = Framework.getService(
                EloraVersionLabelService.class);
        return versionLabelService.getMajor(docModel);
    }

    protected String getMinorToDisplay(DocumentModel docModel) {
        EloraVersionLabelService versionLabelService = Framework.getService(
                EloraVersionLabelService.class);
        return versionLabelService.getMinor(docModel);
    }

    @Override
    public Document doPostSave(CoreSession session, Document doc,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {

        // Check if version label is forced
        if (options.containsKey(
                EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL)) {
            String label = (String) options.get(
                    EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL);
            return doc.checkIn(label, checkinComment);

        } else {
            if (isPostSaveDoingCheckIn(doc, option, options)) {
                incrementByOption(doc, option);
                return doc.checkIn(null, checkinComment); // auto-label
            }
        }

        return null;
    }

    @Override
    protected void followTransitionByOption(CoreSession session, Document doc,
            Map<String, Serializable> options) {
        // TODO Hau alperrik dau? Oin beti checkout egiten da editetan hasi ahal
        // izateko, eta aldaketa hau zan editatzerakuen Save (no increment)
        // aukeratzerakuen checkout egiten zalako. Baina suposatzen da kasu hori
        // ez dala errepikatzen. (Inportazioan???)
        // TODO Hau aldatzen bada, EloraDocumentHelper.checkOut be aldatu behar
        // da.
        String lifecycleState = doc.getLifeCycleState();
        // Originally, obsolete is also added
        // if (EloraLifeCycleConstants.APPROVED.equals(lifecycleState) ||
        // EloraLifeCycleConstants.OBSOLETE.equals(lifecycleState)) {
        if (EloraLifeCycleConstants.APPROVED.equals(lifecycleState)) {
            doc.followTransition(
                    EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY);
            if (session != null) {
                // Send an event to notify that the document state has changed
                sendEvent(session, doc, lifecycleState, options);
            }
        }
    }

}
