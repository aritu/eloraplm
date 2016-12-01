package com.aritu.eloraplm.versioning;

import java.io.Serializable;
import java.util.Map;

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
        EloraVersionLabelService versionLabelService = Framework.getService(
                EloraVersionLabelService.class);
        String label;
        try {
            label = getMajorToDisplay(docModel) + "."
                    + getMinorToDisplay(docModel);
            if (docModel.isCheckedOut()
                    && !versionLabelService.getZeroVersion().equals(label)) {
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
    public void doPostCreate(Document doc, Map<String, Serializable> options) {

        // ----------------------------------------------------------------------------
        // SECTION ADDED FOR IMPORT PROCESS
        // -----------------------------------------------------------------------------
        // Check that we are in an IMPORTATION PROCESS (ACTION_SCOPE = IMPORT)
        if (options.containsKey(EloraGeneralConstants.CONTEXT_KEY_ACTION_SCOPE)
                && options.get(
                        EloraGeneralConstants.CONTEXT_KEY_ACTION_SCOPE).equals(
                                EloraGeneralConstants.CONTEXT_VAL_IMPORT)) {

            // Nothing to do, since the major and the minor are already set
            return;
        }

        // ----------------------------------------------------------------------------
        // DEFAULT BEHAVIOR
        // -----------------------------------------------------------------------------
        if (doc.isVersion() || doc.isProxy()) {
            return;
        }
        setInitialVersion(doc);
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option,
            String checkinComment, Map<String, Serializable> options) {

        // ----------------------------------------------------------------------------
        // SECTION ADDED FOR IMPORT PROCESS
        // -----------------------------------------------------------------------------
        // Check that we are in an IMPORTATION PROCESS (ACTION_SCOPE = IMPORT)
        if (options.containsKey(EloraGeneralConstants.CONTEXT_KEY_ACTION_SCOPE)
                && options.get(
                        EloraGeneralConstants.CONTEXT_KEY_ACTION_SCOPE).equals(
                                EloraGeneralConstants.CONTEXT_VAL_IMPORT)) {

            /* if (isPostSaveDoingCheckIn(doc, option, options)) { */

            String label = null;
            if (options.containsKey(
                    EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL)) {
                label = (String) options.get(
                        EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL);
            }
            return doc.checkIn(label, checkinComment); // auto-label
            /* } */
        }

        // ----------------------------------------------------------------------------
        // DEFAULT BEHAVIOR
        // -----------------------------------------------------------------------------
        if (isPostSaveDoingCheckIn(doc, option, options)) {
            incrementByOption(doc, option);
            return doc.checkIn(null, checkinComment); // auto-label
        }
        return null;

    }

    @Override
    protected void followTransitionByOption(Document doc,
            VersioningOption option) {
        // TODO Hau aldatzen bada, EloraDocumentHelper.checkOut be aldatu behar
        // da.
        String lifecycleState = doc.getLifeCycleState();
        if (EloraLifeCycleConstants.CAD_APPROVED.equals(lifecycleState)) {
            doc.followTransition(
                    EloraLifeCycleConstants.CAD_TRANS_BACK_TO_PRELIMINARY);
        }
    }
}
