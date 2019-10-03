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
package com.aritu.eloraplm.pdm.promote.executer.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.config.util.PropagationConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.promote.treetable.PromoteNodeData;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ApprovePromoteExecuterService extends PromoteExecuterService {

    private static final Log log = LogFactory.getLog(
            ObsoletePromoteExecuterService.class);

    protected EloraConfigTable relationDescendingPropagationConfig;

    protected Map<String, List<EloraConfigRow>> relationPropagationMap;

    @Override
    public EloraConfigTable getRelationDescendingPropagationConfig() {
        return relationDescendingPropagationConfig;
    }

    @Override
    public Map<String, List<EloraConfigRow>> getRelationPropagationMap() {
        return relationPropagationMap;
    }

    public ApprovePromoteExecuterService() {
        loadConfigurations();
    }

    private void loadConfigurations() {
        relationDescendingPropagationConfig = PropagationConfig.approveDescendingPropagationConfig;
        relationPropagationMap = PropagationConfig.approvePropagationMap;
    }

    @Override
    public void processPromote(TreeNode node, String transition,
            String finalState, EloraConfigTable lifeCycleStatesConfig,
            EloraDocumentRelationManager eloraDocumentRelationManager)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

        for (TreeNode child : node.getChildren()) {
            processPromote(child, transition, finalState, lifeCycleStatesConfig,
                    eloraDocumentRelationManager);
        }
        PromoteNodeData nodeData = (PromoteNodeData) node.getData();
        boolean alreadyPromoted = nodeData.getNodeInfo().getAlreadyPromoted();
        boolean isPropagated = nodeData.getNodeInfo().getIsPropagated();

        if (!alreadyPromoted && isPropagated) {
            DocumentModel doc = nodeData.getData();
            doc.refresh();
            // TODO: Comprobamos por si aparece dos veces en el arbol y se ha
            // promocionado anteriormente. Se puede mirar si creando un DAG como
            // en los procesos masivos se puede evitar esto ordenando y
            // ejecutando cada documento solo una vez
            boolean updatedAlreadyPromoted = PromoteHelper.isAlreadyPromoted(
                    doc, finalState, lifeCycleStatesConfig);
            if (!updatedAlreadyPromoted) {
                CoreSession session = doc.getCoreSession();
                EloraDocumentHelper.lockDocument(nodeData.getData());
                doPromote(doc, transition, eloraDocumentRelationManager,
                        session);
                unlockDocument(doc, session);
                log.trace("[processPromote] Document promoted: |" + doc.getId()
                        + "|");
            }
        }
    }

    @Override
    public LinkedHashMap<String, String> getVersionMap(DocumentModel doc,
            Statement stmt, boolean isSpecial, int level)
            throws EloraException {

        if (!doc.isVersion()) {
            // In versionMap there should never be a wc uid
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc == null) {
                throw new EloraException("Document |" + doc.getId()
                        + "| has no base version. Probably because it has no AVs.");
            }
            doc = baseDoc;
        }

        LinkedHashMap<String, String> versionMap = new LinkedHashMap<String, String>();
        if (level <= 1) {
            // Get only documents from major version
            // TODO: Si major version esta released no tendria que sacar nada.
            // Ahora sigue sacando todos
            DocumentModelList majorDocList = EloraDocumentHelper.getMajorVersionDocList(
                    doc.getRef(), doc.getCoreSession());
            for (DocumentModel majorDoc : majorDocList) {
                String versionRealUid = majorDoc.getId();
                String versionLabel = majorDoc.getVersionLabel();
                versionMap.put(versionRealUid, versionLabel);
            }
        } else {
            // TODO: En estos momentos aqui no entra ya que solo dejamos cambiar
            // la version del primer nodo
            DocumentModelList promoteDocList = EloraDocumentHelper.getPromotableDocList(
                    doc.getRef(), stmt, isSpecial, doc.getCoreSession());
            for (DocumentModel promoteDoc : promoteDocList) {
                String versionRealUid = promoteDoc.getId();
                String versionLabel = promoteDoc.getVersionLabel();
                versionMap.put(versionRealUid, versionLabel);
            }

        }

        // TODO: En la opción asStored puede que un conjunto tenga una pieza
        // en
        // una versión que no se puede promocionar. Aún así, lo mostramos
        // para
        // que se vea cuál es la estructura real. Esto dará un KO. Lo
        // añadimos
        // al final, no se ordena en la lista ya que puede afectar al
        // rendimiento andar ordenando. Mirar si hay otra forma más
        // eficiente de
        // sacar ordenado la lista de versiones
        if (!versionMap.containsKey(doc.getId())) {
            versionMap.put(doc.getId(), doc.getVersionLabel());
        }

        return versionMap;
    }

    // public void processPromoteFuturo(TreeNode node, String transition) {
    // for (TreeNode child : node.getChildren()) {
    // processPromote(child, transition);
    // }
    // PromoteNodeData nodeData = (PromoteNodeData) node.getData();
    // boolean alreadyPromoted = nodeData.getNodeInfo().getAlreadyPromoted();
    // if (!alreadyPromoted) {
    // DocumentModel doc = nodeData.getData();
    // CoreSession session = doc.getCoreSession();
    // lockDocument(nodeData.getData(), session);
    // doPromote(doc, nodeData, transition, session);
    // unlockDocument(doc, session);
    // } else {
    // // TODO: Habrá casos en los que no se tendrá que cambiar la
    // // estructura nunca. Por ejemplo, en los que el parent ya
    // // está promoted. Hay que controlar para ser mas eficientes
    // rebuildRelations(nodeData.getData(), nodeData);
    // }
    // }

    // TODO: Esta funcion puede ser comun para todos y subirlo a
    // PromoteExecuterService. Ahora el rebuildRelations solo se tiene en cuenta
    // en el approve y por eso esta separado. En un futuro habria que añadirlo a
    // todos
    // private void doPromoteFuturo(DocumentModel doc, PromoteNodeData nodeData,
    // String promoteTransition, CoreSession session)
    // throws EloraException {
    // String logInitMsg = "[doPromote] [" + session.getPrincipal().getName()
    // + "] ";
    //
    // doc.followTransition(promoteTransition);
    // rebuildRelations(doc, nodeData);
    //
    // VersionModel version = new VersionModelImpl();
    // version.setId(doc.getId());
    //
    // DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
    // wcDoc = EloraDocumentHelper.restoreToVersion(wcDoc, version,
    // eloraDocumentRelationManager, session);
    // // updateViewer(wcDoc);
    // // updateViewer(doc);
    //
    // // TODO: Hemos decidido no actualizar las relaciones de todos los
    // // subjects que apuntan a cualquier version de este major de este
    // // documento(todos
    // // los statement que tienen como object cualquier version dentro del
    // // major del documento). Por ahora, no queremos cambiar cosas de
    // // otros documentos sin que otro usuario que este utilizando ese
    // // documento se de cuenta
    //
    // log.trace(logInitMsg + "Document promoted: |" + doc.getId() + "|");
    // }

    // private void rebuildRelations(DocumentModel doc, PromoteNodeData
    // nodeData) {
    // // For special relations we don't change structure because we
    // // only paint related docs.
    // if (!nodeData.getIsSpecial()) {
    // // Check if original relation with parent doc has changed (if user
    // // selects a different version)
    // Statement stmt = nodeData.getStmt();
    // if (stmt != null) {
    // // root has stmt = null so you don't have to check relation with
    // // parents
    // DocumentModel asStoredDoc = RelationHelper.getDocumentModel(
    // stmt.getObject(), session);
    // if (!asStoredDoc.getId().equals(doc.getId())) {
    // // If document version has changed we create a new relation
    // // and remove previous one
    // EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(
    // stmt);
    // DocumentModel subject = RelationHelper.getDocumentModel(
    // stmt.getSubject(), session);
    // // Remove previous relation
    // RelationHelper.removeRelation(subject, stmt.getPredicate(),
    // asStoredDoc);
    //
    // // TODO: Se está cambiando las relaciones del padre desde el
    // // hijo. No se hace ningún control sobre el padre y no se si
    // // esto está bien... Por ahora no vemos ningún caso en el
    // // que nos rompa algo
    // eloraDocumentRelationManager.addRelation(session, subject,
    // doc, stmt.getPredicate().getUri(),
    // eloraStmtInfo.getComment(),
    // eloraStmtInfo.getQuantity(), null, null);
    // }
    // }
    // }
    // }

    // private void updateViewer(DocumentModel doc) throws EloraException {
    // try {
    // // Update viewer after promote
    // EloraDocumentHelper.disableVersioningDocument(doc);
    // Blob viewerBlob = ViewerPdfUpdater.createViewer(doc);
    // if (viewerBlob != null) {
    // EloraDocumentHelper.addViewerBlob(doc, viewerBlob);
    // }
    // session.saveDocument(doc);
    // } catch (Exception e) {
    // throw new EloraException(e.getMessage());
    // }
    // }

}
