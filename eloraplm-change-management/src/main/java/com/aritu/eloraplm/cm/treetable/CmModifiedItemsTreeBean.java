package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

import com.sun.faces.util.MessageFactory;

@Name("cmModifiedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmModifiedItemsTreeBean extends CoreTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsTreeBean.class);

    @In(create = true)
    protected transient CmDocImpactedItemsTreeBean cmDocImpactedItemsTreeBean;

    @In(create = true)
    protected transient CmBomImpactedItemsTreeBean cmBomImpactedItemsTreeBean;

    private CmModifiedItemsNodeService nodeService;

    private EloraConfigTable releasedStatesConfig;

    // ---- Properties for managing "Add Modified Item" window ----
    // -- Modified Item Origin
    private String originItemWcUid;

    private String originItemRealUid;

    private Map<String, String> originItemVersionList;

    private DocumentModel originItem;

    private DocumentModel originItemWc;

    private String originItemType;

    // -- Action
    private String action;

    // -- Modified Item Destination
    private String destinationItemWcUid;

    private String destinationItemRealUid;

    private Map<String, String> destinationItemVersionList;

    private DocumentModel destinationItem;

    // -- IsManaged
    private boolean isManaged;

    // ------------------------------------------------------------

    public CmModifiedItemsTreeBean() {
        try {
            releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();

            resetCreateFormValues(true);

        } catch (EloraException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();
            nodeService = new CmModifiedItemsNodeService(documentManager,
                    releasedStatesConfig);
            setRoot(nodeService.getRoot(currentDoc));

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService.saveTree(currentDoc, getRoot());

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.saveRoot"));

            // refresh current treebean and refresh other IMPACT MATRIXES
            resetBeanCache(null);
            cmDocImpactedItemsTreeBean.createRoot();
            cmBomImpactedItemsTreeBean.createRoot();

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.saveRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {

            nodeService.refreshNode(node, triggeredField);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        // log.trace(logInitMsg + "--- EXIT --- ");
    }

    /*public void removeModifiedItem(TreeNode node) {
        log.trace("************ removeModifiedItem");
    
        String logInitMsg = "[removeModifiedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
    
       try {
            DocumentModel cmProcessDM = getCurrentDocument();
            CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) node.getData();
    
            CMHelper.removeModifiedItemFromCMProcess(documentManager,
                    cmProcessDM, nodeData.getOriginItem().getId());
    
            getRoot().getChildren().remove(node);
    
            // refresh other IMPACT MATRIXES
            cmDocImpactedItemsTreeBean.createRoot();
            cmBomImpactedItemsTreeBean.createRoot();
    
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.removeModifiedItem"));
        }
    
        log.trace(logInitMsg + "--- EXIT ---");
    
    }*/

    public void markModifiedNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[markModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleModifiedNodeAsRemoved(true, node);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void unmarkModifiedNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[unmarkModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleModifiedNodeAsRemoved(false, node);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void toggleModifiedNodeAsRemoved(boolean isRemoved, TreeNode node) {
        String logInitMsg = "[toggleModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) node.getData();
        // If it is a new node, remove it completely
        if (nodeData.getIsNew()) {
            getRoot().getChildren().remove(node);
        } else {
            nodeData.setIsRemoved(isRemoved);
            node.getChildren().clear();
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void addModifiedItem() {
        String logInitMsg = "[addModifiedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel cmProcessDM = getCurrentDocument();

            if (CMHelper.existModifiedItemInCMProcess(documentManager,
                    cmProcessDM.getId(), originItemRealUid)) {

                log.trace(logInitMsg
                        + "modified item already exist in process. modifiedItemRealUid =|"
                        + originItemRealUid + "|, cmProcessDMUid=|"
                        + cmProcessDM.getId() + "|");

                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.modifiedItemAlreadyExist"));

            } else {
                setRoot(nodeService.addNewData(getRoot(), originItem,
                        originItemWc, action, destinationItem, isManaged,
                        originItemType));

                resetCreateFormValues(true);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.addModifiedItem"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void validateModificationOriginValue(FacesContext context,
            UIComponent component, Object value) {
        String logInitMsg = "[validateModificationOriginValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        String validationResultMessage = "";

        if (value == null || value.toString().length() == 0) {
            validationResultMessage = "javax.faces.component.UIInput.REQUIRED";
            originItemRealUid = null;
        } else {
            validationResultMessage = nodeService.validateModificationOriginValue(
                    getRoot(), value.toString());
        }

        if (validationResultMessage != null
                && validationResultMessage.length() > 0) {

            // reset form values and form input values (for handling
            // validation exception cases)
            resetCreateFormValues(false);
            resetCreateFormInputValues(component, false);

            // Throw validation exception
            FacesMessage message = MessageFactory.getMessage(context,
                    validationResultMessage);
            throw new ValidatorException(message);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void validateModificationDestinationValue(FacesContext context,
            UIComponent component, Object value) {

        String logInitMsg = "[validateModificationDestinationValue] ";
        log.trace(logInitMsg + "--- ENTER ---");

        String validationResultMessage = "";

        if (value == null || value.toString().length() == 0) {
            validationResultMessage = "javax.faces.component.UIInput.REQUIRED";
            destinationItemRealUid = null;
        }

        if (validationResultMessage != null
                && validationResultMessage.length() > 0) {

            // reset form values and form input values (for handling
            // validation exception cases)
            destinationItem = null;
            isManaged = false;

            resetCreateFormInputValues(component, true);

            // Throw validation exception
            FacesMessage message = MessageFactory.getMessage(context,
                    validationResultMessage);
            throw new ValidatorException(message);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void cancelActionListener(ActionEvent event) {
        String logInitMsg = "[cancelActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        resetCreateFormValues(true);

        resetCreateFormInputValues(event.getComponent(), true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCreateFormValues(true);

        createRoot();
    }

    // ---- Methods for managing "Add Modified Item" window ----
    public String getOriginItemWcUid() {
        return originItemWcUid;
    }

    public void setOriginItemWcUid(String originItemWcUid) {
        String logInitMsg = "[setOriginItemWcUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            resetCreateFormValues(true);

            this.originItemWcUid = originItemWcUid;

            if (originItemWcUid != null) {

                setOriginItemWc(documentManager.getDocument(
                        new IdRef(originItemWcUid)));

                // Calculate the Released version list for the selected origin
                // item and retrieve its WC
                Map<String, String> releasedVersionList = CMHelper.calculateReleasedVersionList(
                        documentManager, originItemWcUid, true);
                if (releasedVersionList.containsKey(
                        CMConstants.DEFAULT_SELECTED_KEY)) {
                    String selectedVersionRealUid = releasedVersionList.get(
                            CMConstants.DEFAULT_SELECTED_KEY);
                    releasedVersionList.remove(
                            CMConstants.DEFAULT_SELECTED_KEY);
                    setOriginItemRealUid(selectedVersionRealUid);
                }
                setOriginItemVersionList(releasedVersionList);
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    public String getOriginItemRealUid() {
        return originItemRealUid;
    }

    public void setOriginItemRealUid(String originItemRealUid) {
        String logInitMsg = "[setOriginItemRealUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.originItemRealUid = originItemRealUid;

            if (originItemRealUid != null) {
                // retrieve the originItem from its id
                setOriginItem(documentManager.getDocument(
                        new IdRef(originItemRealUid)));
                // initialize action to default value
                setAction(null);
            } else {
                setOriginItem(null);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    public Map<String, String> getOriginItemVersionList() {
        return originItemVersionList;
    }

    public void setOriginItemVersionList(
            Map<String, String> originItemVersionList) {
        this.originItemVersionList = originItemVersionList;
    }

    public DocumentModel getOriginItem() {
        return originItem;
    }

    public void setOriginItem(DocumentModel originItem) {
        this.originItem = originItem;
        if (originItem != null) {
            setOriginItemType(CMHelper.getItemType(originItem));
        }
    }

    public DocumentModel getOriginItemWc() {
        return originItemWc;
    }

    public void setOriginItemWc(DocumentModel originItemWc) {
        this.originItemWc = originItemWc;
    }

    public String getOriginItemType() {
        return originItemType;
    }

    public void setOriginItemType(String originItemType) {
        this.originItemType = originItemType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        if (action == null) {
            action = CMConstants.ACTION_CHANGE;
        }
        this.action = action;

        if (action.equals(CMConstants.ACTION_REMOVE)) {
            setDestinationItem(null);
            setIsManaged(true);

            // Clear the properties for managing "Replace destination item"
            clearReplaceDestinationSelectionElements();

        } else if (action.equals(CMConstants.ACTION_CHANGE)) {
            setDestinationItem(originItemWc);
            setIsManaged(false);

            // Clear the properties for managing "Replace destination item"
            clearReplaceDestinationSelectionElements();

        } else if (action.equals(CMConstants.ACTION_REPLACE)) {
            setDestinationItemWcUid(null);
            setDestinationItemRealUid(null);
            isManaged = false;
        }

    }

    private void clearReplaceDestinationSelectionElements() {
        destinationItemWcUid = null;
        destinationItemRealUid = null;
        destinationItemVersionList = new HashMap<String, String>();
    }

    public String getDestinationItemWcUid() {
        return destinationItemWcUid;
    }

    public void setDestinationItemWcUid(String destinationItemWcUid) {
        String logInitMsg = "[setDestinationItemWcUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.destinationItemWcUid = destinationItemWcUid;

            if (destinationItemWcUid != null) {
                // Calculate the version list for the selected destination item
                Map<String, String> versionList = CMHelper.calculateVersionList(
                        documentManager, destinationItemWcUid, true);
                if (versionList.containsKey(CMConstants.DEFAULT_SELECTED_KEY)) {
                    String selectedVersionRealUid = versionList.get(
                            CMConstants.DEFAULT_SELECTED_KEY);
                    versionList.remove(CMConstants.DEFAULT_SELECTED_KEY);
                    setDestinationItemRealUid(selectedVersionRealUid);
                }
                setDestinationItemVersionList(versionList);
            } else {
                setDestinationItemVersionList(new HashMap<String, String>());
            }

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public String getDestinationItemRealUid() {
        return destinationItemRealUid;
    }

    public void setDestinationItemRealUid(String destinationItemRealUid) {
        String logInitMsg = "[setDestinationItemRealUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.destinationItemRealUid = destinationItemRealUid;

            if (destinationItemRealUid != null) {
                // retrieve the destinationItem from its id
                setDestinationItem(documentManager.getDocument(
                        new IdRef(destinationItemRealUid)));
            } else {
                destinationItem = null;
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }

    }

    public Map<String, String> getDestinationItemVersionList() {
        return destinationItemVersionList;
    }

    public void setDestinationItemVersionList(
            Map<String, String> destinationItemVersionList) {
        this.destinationItemVersionList = destinationItemVersionList;
    }

    public DocumentModel getDestinationItem() {
        return destinationItem;
    }

    public void setDestinationItem(DocumentModel destinationItem) {
        this.destinationItem = destinationItem;
    }

    public boolean getIsManaged() {
        return isManaged;
    }

    public void setIsManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    private void resetCreateFormValues(boolean includeOriginItem) {

        if (includeOriginItem) {
            originItemWcUid = null;

            originItemRealUid = null;

            originItemVersionList = new HashMap<String, String>();

            originItem = null;

            originItemWc = null;

            originItemType = null;
        }

        action = null;

        destinationItemWcUid = null;

        destinationItemRealUid = null;

        destinationItemVersionList = new HashMap<String, String>();

        destinationItem = null;

        isManaged = false;
    }

    private void resetCreateFormInputValues(UIComponent component,
            boolean includeOriginItem) {

        if (includeOriginItem) {
            // reset originItem component
            UIInput originSuggestInputComponent = (UIInput) component.findComponent(
                    "nxw_eloraSingleDocumentSuggestion_select2");
            if (originSuggestInputComponent != null) {
                EditableValueHolder originSuggestInput = originSuggestInputComponent;
                if (originSuggestInput != null) {
                    originSuggestInput.resetValue();
                    // originSuggestInput.setValue(null);
                }
            }

            // reset originItemRealUid component
            UIInput originItemRealUidInputComponent = (UIInput) component.findComponent(
                    "originItemRealUid");
            if (originItemRealUidInputComponent != null) {
                EditableValueHolder originItemRealUidInput = originItemRealUidInputComponent;
                if (originItemRealUidInput != null) {
                    originItemRealUidInput.resetValue();
                }
            }
        }

        // reset action component
        UIInput actionInputComponent = (UIInput) component.findComponent(
                "nxw_selectOneDirectory_2");
        if (actionInputComponent != null) {
            EditableValueHolder actionInput = actionInputComponent;
            if (actionInput != null) {
                actionInput.resetValue();
            }
        }

        // reset destinationItemSuggestInputComponent
        UIInput destinationItemSuggestInputComponent = (UIInput) component.findComponent(
                "nxw_eloraSingleDocumentSuggestion_1_select2");
        if (destinationItemSuggestInputComponent != null) {
            EditableValueHolder destinationItemSuggestInput = destinationItemSuggestInputComponent;
            if (destinationItemSuggestInput != null) {
                destinationItemSuggestInput.resetValue();
            }
        }

        // reset destinationItemRealUid component
        UIInput destinationItemRealUidInputComponent = (UIInput) component.findComponent(
                "destinationItemRealUid");
        if (destinationItemRealUidInputComponent != null) {
            EditableValueHolder destinationItemRealUidInput = destinationItemRealUidInputComponent;
            if (destinationItemRealUidInput != null) {
                destinationItemRealUidInput.resetValue();
            }
        }

        // reset isManaged component
        UIInput isManagedInputComponent = (UIInput) component.findComponent(
                "nxw_checkbox_1");
        if (isManagedInputComponent != null) {
            EditableValueHolder isManagedInput = isManagedInputComponent;
            if (isManagedInput != null) {
                isManagedInput.resetValue();
            }
        }
    }

    // ---------------------------------------------------------
}