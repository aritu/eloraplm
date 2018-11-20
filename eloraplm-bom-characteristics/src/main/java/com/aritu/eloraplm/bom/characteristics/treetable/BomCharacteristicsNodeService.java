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
package com.aritu.eloraplm.bom.characteristics.treetable;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.characteristics.BomCharacteristic;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsQueryFactory;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsQueryResultFactory;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class BomCharacteristicsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected String bomType;

    protected Map<String, String> messages;

    public BomCharacteristicsNodeService(CoreSession session, String bomType,
            Map<String, String> messages) throws EloraException {
        this.session = session;
        this.bomType = bomType;
        this.messages = messages;
        nodeId = 0;
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.treetable.NodeService#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {

        String logInitMsg = "[" + bomType + "] [getRoot] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDoc = (DocumentModel) parentObject;

        if (!currentDoc.getType().equals(bomType)) {
            throw new EloraException(
                    "First level document of the tree must be a " + bomType
                            + ".");
        }

        int level = 0;
        BomCharacteristicsNodeData nodeData = new BomCharacteristicsNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createBomCharacteristicsRootTree(currentDoc, bomType, root,
                level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createBomCharacteristicsRootTree(DocumentModel currentDoc,
            String itemType, TreeNode root, int level) throws EloraException {

        String logInitMsg = "[" + bomType
                + "] [createBomCharacteristicsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult it = null;
        try {
            String query = BomCharacteristicsQueryFactory.getBomCharacteristicsByDocumentQuery(
                    currentDoc.getId());
            it = session.queryAndFetch(query, NXQL.NXQL);

            String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

            if (it.size() > 0) {
                for (Map<String, Serializable> map : it) {

                    String bomCharacteristicId = (String) map.get(
                            pfx + "/*1/bomCharacteristicId");

                    String classification = (String) map.get(
                            pfx + "/*1/classification");
                    Long order = (Long) map.get(pfx + "/*1/order");
                    String bomCharacteristicMaster = (String) map.get(
                            pfx + "/*1/bomCharacMaster");

                    Date bomCharacteristicMasterLastModified = null;
                    GregorianCalendar bomCharacteristicMasterLastModifiedCalendar = (GregorianCalendar) map.get(
                            pfx + "/*1/bomCharacMasterLastModified");
                    if (bomCharacteristicMasterLastModifiedCalendar != null) {
                        // Taken from Nuxeo code:
                        // -------------------------------------------------
                        // remove milliseconds as they are not stored in some
                        // databases, which could make the comparison fail just
                        // after a document creation (see NXP-8783)
                        // -------------------------------------------------
                        bomCharacteristicMasterLastModifiedCalendar.set(
                                Calendar.MILLISECOND, 0);
                        bomCharacteristicMasterLastModified = bomCharacteristicMasterLastModifiedCalendar.getTime();
                    }

                    String title = (String) map.get(pfx + "/*1/title");
                    String description = (String) map.get(
                            pfx + "/*1/description");
                    String type = (String) map.get(pfx + "/*1/type");

                    Long numberMaxIntegerPlaces = null;
                    Long numberMaxDecimalPlaces = null;
                    String numberDefaultValue = null;
                    String numberValue = null;
                    Long stringMaxLength = null;
                    String stringDefaultValue = null;
                    String stringValue = null;
                    Date dateDefaultValue = null;
                    Date dateValue = null;
                    Boolean booleanDefaultValue = null;
                    Boolean booleanValue = null;
                    List<Map<String, String>> listContent = null;
                    String listDefaultValue = null;
                    String listValue = null;

                    switch (type) {
                    case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
                        numberMaxIntegerPlaces = (Long) map.get(
                                pfx + "/*1/numberMaxIntegerPlaces");
                        numberMaxDecimalPlaces = (Long) map.get(
                                pfx + "/*1/numberMaxDecimalPlaces");
                        numberDefaultValue = (String) map.get(
                                pfx + "/*1/numberDefaultValue");
                        numberValue = (String) map.get(pfx + "/*1/numberValue");
                        break;

                    case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
                        stringMaxLength = (Long) map.get(
                                pfx + "/*1/stringMaxLength");
                        stringDefaultValue = (String) map.get(
                                pfx + "/*1/stringDefaultValue");
                        stringValue = (String) map.get(pfx + "/*1/stringValue");
                        break;

                    case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
                        GregorianCalendar dateDefaultValueCalendar = (GregorianCalendar) map.get(
                                pfx + "/*1/dateDefaultValue");
                        if (dateDefaultValueCalendar != null) {
                            dateDefaultValue = dateDefaultValueCalendar.getTime();
                        }
                        GregorianCalendar dateValueCalendar = (GregorianCalendar) map.get(
                                pfx + "/*1/dateValue");
                        if (dateValueCalendar != null) {
                            dateValue = dateValueCalendar.getTime();
                        }
                        break;

                    case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
                        booleanDefaultValue = (Boolean) map.get(
                                pfx + "/*1/booleanDefaultValue");
                        booleanValue = (Boolean) map.get(
                                pfx + "/*1/booleanValue");
                        break;

                    case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
                        // Retrieve the list content
                        listContent = BomCharacteristicsQueryResultFactory.getBomCharacteristicListContent(
                                session, currentDoc.getId(),
                                bomCharacteristicId);

                        listDefaultValue = (String) map.get(
                                pfx + "/*1/listDefaultValue");
                        listValue = (String) map.get(pfx + "/*1/listValue");
                        break;
                    }

                    String unit = (String) map.get(pfx + "/*1/unit");
                    Boolean showInReport = (Boolean) map.get(
                            pfx + "/*1/showInReport");
                    Long orderInReport = (Long) map.get(
                            pfx + "/*1/orderInReport");
                    Boolean required = (Boolean) map.get(pfx + "/*1/required");
                    Boolean includeInTitle = (Boolean) map.get(
                            pfx + "/*1/includeInTitle");
                    Boolean unmodifiable = (Boolean) map.get(
                            pfx + "/*1/unmodifiable");

                    String messageType = (String) map.get(
                            pfx + "/*1/messageType");
                    String message = (String) map.get(pfx + "/*1/message");

                    BomCharacteristic bomCharacteristic = new BomCharacteristic(
                            bomCharacteristicId, classification,
                            bomCharacteristicMaster,
                            bomCharacteristicMasterLastModified, order, title,
                            description, type, numberMaxIntegerPlaces,
                            numberMaxDecimalPlaces, numberDefaultValue,
                            numberValue, stringMaxLength, stringDefaultValue,
                            stringValue, dateDefaultValue, dateValue,
                            booleanDefaultValue, booleanValue, listContent,
                            listDefaultValue, listValue, unit, showInReport,
                            orderInReport, required, includeInTitle,
                            unmodifiable, messageType, message);

                    String classificationLabel = BomCharacteristicsHelper.getBomClassificationLabel(
                            bomType, classification);

                    Locale locale = EloraMessageHelper.getLocale(
                            currentDoc.getCoreSession());

                    String bomCharacteristicConstraints = BomCharacteristicsHelper.getBomCharacteristicConstraints(
                            bomCharacteristic, messages, locale);

                    BomCharacteristicsNodeData nodeData = new BomCharacteristicsNodeData(
                            String.valueOf(nodeId), level, false, false, false,
                            bomCharacteristic, classificationLabel,
                            bomCharacteristicConstraints);

                    nodeId++;

                    TreeNode node = new DefaultTreeNode(nodeData, root);
                    node.setExpanded(true);
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            it.close();
        }
        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root)
            throws EloraException {

        // TODO::: gehitu try/catch

        String logInitMsg = "[" + bomType + "] [saveTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<BomCharacteristic> bomCharacteristicsToBeAdded = new LinkedList<BomCharacteristic>();
        // only to be sure that same BOM Characteristic Master is not added more
        // than once
        List<String> bomCharacteristicMasterUidsToBeAdded = new LinkedList<String>();

        List<String> bomCharacteristicIdsToBeRemoved = new LinkedList<String>();

        HashMap<String, BomCharacteristic> changedBomCharacteristics = new HashMap<String, BomCharacteristic>();

        for (TreeNode childNode : root.getChildren()) {
            BomCharacteristicsNodeData nodeData = (BomCharacteristicsNodeData) childNode.getData();
            BomCharacteristic nodeDataBomCharacteristic = nodeData.getBomCharacteristic();
            String nodeDataBomCharacteristicId = nodeDataBomCharacteristic.getBomCharacteristicId();
            String nodeDataBomCharacteristicMasterUid = nodeDataBomCharacteristic.getBomCharacteristicMaster();

            if (nodeData.getIsNew()) {
                if (!bomCharacteristicMasterUidsToBeAdded.contains(
                        nodeDataBomCharacteristicMasterUid)) {
                    bomCharacteristicMasterUidsToBeAdded.add(
                            nodeDataBomCharacteristicMasterUid);

                    bomCharacteristicsToBeAdded.add(nodeDataBomCharacteristic);
                }
            } else if (nodeData.getIsRemoved()) {
                // be sure that the nodeData is not required
                if (!nodeDataBomCharacteristic.getRequired()) {
                    bomCharacteristicIdsToBeRemoved.add(
                            nodeDataBomCharacteristicId);
                }

            } else if (nodeData.getIsModified()) {
                // Remove message related info when saving.
                nodeDataBomCharacteristic.setMessageType(null);
                nodeDataBomCharacteristic.setMessage(null);
                changedBomCharacteristics.put(nodeDataBomCharacteristicId,
                        nodeDataBomCharacteristic);
            } else {
                // Even if the node has not been manually modified, if it
                // contains any message, it should be removed
                if ((nodeDataBomCharacteristic.getMessageType() != null
                        && !nodeDataBomCharacteristic.getMessageType().isEmpty())
                        || (nodeDataBomCharacteristic.getMessage() != null
                                && !nodeDataBomCharacteristic.getMessage().isEmpty())) {
                    nodeDataBomCharacteristic.setMessageType(null);
                    nodeDataBomCharacteristic.setMessage(null);
                    changedBomCharacteristics.put(nodeDataBomCharacteristicId,
                            nodeDataBomCharacteristic);
                }
            }
        }

        BomCharacteristicsHelper.saveBomCharacteristicsChangesInDocument(
                session, currentDoc, bomCharacteristicIdsToBeRemoved,
                bomCharacteristicsToBeAdded, changedBomCharacteristics);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected boolean verifyRequiredFields(TreeNode root) {
        boolean result = true;

        for (TreeNode childNode : root.getChildren()) {
            BomCharacteristicsNodeData nodeData = (BomCharacteristicsNodeData) childNode.getData();
            BomCharacteristic nodeDataBomCharacteristic = nodeData.getBomCharacteristic();
            boolean isRequired = nodeDataBomCharacteristic.getRequired();

            if (isRequired) {
                if (nodeData.getIsRemoved()) {
                    result = false;
                    nodeDataBomCharacteristic.setMessageType(
                            BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR);
                    nodeDataBomCharacteristic.setMessage(messages.get(
                            "javax.faces.component.UIInput.REQUIRED"));
                } else {
                    if (BomCharacteristicsHelper.verifyBomCharacteristicRequiredConstraint(
                            nodeDataBomCharacteristic) == false) {
                        result = false;
                        nodeDataBomCharacteristic.setMessageType(
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR);
                        nodeDataBomCharacteristic.setMessage(messages.get(
                                "javax.faces.component.UIInput.REQUIRED"));
                        nodeData.setBomCharacteristic(
                                nodeDataBomCharacteristic);
                    }
                }
            }
            if (BomCharacteristicsHelper.verifyBomCharacteristicOrderInReportConstraint(
                    nodeDataBomCharacteristic) == false) {
                result = false;
                nodeDataBomCharacteristic.setMessageType(
                        BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR);
                nodeDataBomCharacteristic.appendMessage(messages.get(
                        "message.error.bomcharac.saveCharacteristic.orderInReportRequired"));
            }
        }
        return result;
    }

    public TreeNode loadCharacteristicMasters(DocumentModel currentDoc,
            TreeNode root) throws EloraException {
        String logInitMsg = "[" + bomType + "] [loadCharacteristicMasters] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            // ###########################################################################
            // ------------ STEP 1:
            // Retrieve characteristic masters related to the document (in
            // function of the document's classification)
            // ###########################################################################
            Map<String, BomCharacteristic> classificationRelatedBomCharacteristicMastersMap = BomCharacteristicsHelper.retrieveBomDocumentRelatedBomCharacteristicMasters(
                    currentDoc);

            // ###########################################################################
            // ------------ STEP 2:
            // Compare and merge loaded characteristic masters with the
            // characteristics already stored in the document
            // ###########################################################################
            // Construct a list with current Bom Characteristics
            List<BomCharacteristic> currentBomCharacteristics = new LinkedList<BomCharacteristic>();
            for (TreeNode childNode : root.getChildren()) {
                BomCharacteristicsNodeData nodeData = (BomCharacteristicsNodeData) childNode.getData();
                BomCharacteristic currentBomCharacteristic = nodeData.getBomCharacteristic();
                currentBomCharacteristics.add(currentBomCharacteristic);
            }

            List<BomCharacteristic> newBomCharacteristics = BomCharacteristicsHelper.mergeLoadedCharacteristicMastersWithCurrentCharacteristics(
                    classificationRelatedBomCharacteristicMastersMap,
                    currentBomCharacteristics);

            // ###########################################################################
            // ------------ STEP 3:
            // Set and save new structure
            // ###########################################################################
            BomCharacteristicsHelper.saveBomCharacteriticsListInDocument(
                    session, currentDoc, newBomCharacteristics);

            log.trace(logInitMsg + "--- EXIT ---");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {

        /* String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");*/

        BomCharacteristicsNodeData nodeData = (BomCharacteristicsNodeData) node.getData();
        nodeData.setIsModified(true);

        /* log.trace(logInitMsg + "triggeredField = |" + triggeredField + "|");

        log.trace(logInitMsg + "--- EXIT ---");*/
    }

}
