package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.IMOptions;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraExceptionWithStoredMessage;

@Name("modifiedItemsActions")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
public class ModifiedItemsActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ModifiedItemsActionBean.class);

    protected boolean imAlreadyExists;

    // ------------------------------------------------------------
    // ---- Properties for managing "Edit Modified Item" window ----
    // -- Modified Item Origin

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient ModifiedItemsTreeBean cmModifiedsTreeBean;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public void calculateImpactMatrix(IMOptions imOptions) {
        String logInitMsg = "[calculateImpactMatrix] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        /*
         * imOptions = CMHelper.loadIMOptions(
         * navigationContext.getCurrentDocument());
         */

        if (!cmModifiedsTreeBean.getIsDirty()) {
            try {
                DocumentModel currentDoc = navigationContext.getCurrentDocument();

                ChangeManagementService cmService = Framework.getService(
                        ChangeManagementService.class);

                imOptions.setCreateDocIM(false);
                imOptions.setCreateItemIM(false);
                cmService.calculateImpactMatrix(documentManager, currentDoc,
                        imOptions, facesMessages, messages);

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.treetable.calculateImpactMatrix"));
                Events.instance().raiseEvent(
                        CMEventNames.CM_IMPACT_MATRIX_CALCULATED);

                // Nuxeo event
                EloraEventHelper.fireEvent(
                        CMEventNames.CM_IMPACT_MATRIX_CALCULATED,
                        navigationContext.getCurrentDocument());

            } catch (EloraExceptionWithStoredMessage e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(e.getStoredMessage()));
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.treetable.calculateImpactMatrix"));
            }
        } else {
            log.trace(logInitMsg + "Unsaved changes.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void removeImpactMatrix() {
        String logInitMsg = "[removeImpactMatrix] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            if (!cmModifiedsTreeBean.getIsDirty()) {
                DocumentModel cmProcess = navigationContext.getCurrentDocument();

                ArrayList<HashMap<String, Object>> currentImpactMatrix = new ArrayList<HashMap<String, Object>>();
                currentImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.IMPACTED_ITEM_LIST);

                if (currentImpactMatrix.size() > 0) {
                    for (int i = 0; i < currentImpactMatrix.size(); ++i) {
                        HashMap<String, Object> impactItem = currentImpactMatrix.get(i);

                        if (impactItem.get("refNodeId") == null && impactItem.get("originItem") != null) {
                            DocumentModel wcDoc = documentManager.getDocument(
                                    new IdRef((String) impactItem.get("originItemWc")));

                            if (wcDoc != null && !wcDoc.isCheckedOut()
                                    && EloraDocumentHelper.isLockedByUserOrAdmin(wcDoc,
                                            documentManager)) {
                                wcDoc.removeLock();
                            }
                        }
                    }
                }

                cmProcess.setPropertyValue(
                        CMMetadataConstants.IMPACTED_ITEM_LIST, null);

                CMHelper.setIMOptionsToDefault(cmProcess);

                documentManager.saveDocument(cmProcess);
                documentManager.save();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.treetable.removeImpactMatrix"));

                Events.instance().raiseEvent(
                        CMEventNames.CM_IMPACT_MATRIX_CALCULATED);

                // Nuxeo event
                EloraEventHelper.fireEvent(
                        CMEventNames.CM_IMPACT_MATRIX_CALCULATED,
                        navigationContext.getCurrentDocument());
            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.removeImpactMatrix"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void setImAlreadyExists(boolean imAlreadyExists) {
        this.imAlreadyExists = imAlreadyExists;
    }

    public boolean getImAlreadyExists() {
        imAlreadyExists = CMHelper.IMAlreadyExists(
                navigationContext.getCurrentDocument());
        return imAlreadyExists;
    }

}
