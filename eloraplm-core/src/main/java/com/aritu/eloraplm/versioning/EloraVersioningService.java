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
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

public class EloraVersioningService extends StandardVersioningService {

    @Override
    public String getVersionLabel(DocumentModel docModel) {
        String label;
        try {
            VersionLabelService vls = Framework.getService(
                    VersionLabelService.class);
            label = vls.getVersionLabel(docModel);
        } catch (PropertyNotFoundException e) {
            label = "";
        }
        return label;
    }

    @Override
    public Document doPostSave(CoreSession session, Document doc,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {

        // Check if version label is forced
        if (options.containsKey(
                EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CHECKIN)) {
            String label = (String) options.get(
                    EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CHECKIN);
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
        boolean isReleasedState = EloraDocumentHelper.isReleasedState(
                lifecycleState);
        // Originally, obsolete is also added
        // if (EloraLifeCycleConstants.APPROVED.equals(lifecycleState) ||
        // EloraLifeCycleConstants.OBSOLETE.equals(lifecycleState)) {
        if (isReleasedState && doc.getAllowedStateTransitions().contains(
                EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY)) {
            doc.followTransition(
                    EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY);
            if (session != null) {
                // Send an event to notify that the document state has changed
                sendEvent(session, doc, lifecycleState, options);
            }
        }
    }

}
