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
import com.aritu.eloraplm.pdm.promote.constants.PromoteConstants;
import com.aritu.eloraplm.pdm.promote.treetable.PromoteNodeData;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class ObsoletePromoteExecuterService extends PromoteExecuterService {

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

    public ObsoletePromoteExecuterService() {
        loadConfigurations();
    }

    private void loadConfigurations() {
        relationDescendingPropagationConfig = PropagationConfig.obsoleteDescendingPropagationConfig;
        relationPropagationMap = PropagationConfig.obsoletePropagationMap;
    }

    @Override
    public void processPromote(TreeNode node, String transition,
            String finalState,
            EloraDocumentRelationManager eloraDocumentRelationManager)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {
        PromoteNodeData nodeData = (PromoteNodeData) node.getData();
        if (nodeData.getIsPropagated()
                && nodeData.getResult().equals(PromoteConstants.RESULT_OK)) {

            DocumentModel doc = nodeData.getData();
            doc.refresh();
            // TODO: Comprobamos por si se aparece dos veces en el arbol y se ha
            // promocionado anteriormente. Se puede mirar si creando un DAG como
            // en los procesos masivos se puede evitar esto ordenando y
            // ejecutando cada documento solo una vez
            boolean updatedAlreadyPromoted = PromoteHelper.isAlreadyPromoted(
                    doc, finalState);
            if (!updatedAlreadyPromoted) {
                CoreSession session = doc.getCoreSession();
                EloraDocumentHelper.lockDocument(doc);
                doPromote(doc, transition, eloraDocumentRelationManager,
                        session);
                unlockDocument(doc, session);
                log.trace("[processPromote] Document promoted: |" + doc.getId()
                        + "|");
            }
        }

        for (TreeNode child : node.getChildren()) {
            processPromote(child, transition, finalState,
                    eloraDocumentRelationManager);
        }
    }

    @Override
    public LinkedHashMap<String, String> getVersionMap(DocumentModel doc,
            Statement stmt, boolean isSpecial, int level)
            throws EloraException {
        // TODO: Hay parametros que sobran pero ahora se mantienen para que
        // coincida con el metodo abstract. Esta mal y en un futuro hay que ver
        // si todas estas clases estan bien separadas

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
        DocumentModelList obsoletableDocList = EloraDocumentHelper.getReleasedAndLatestVersions(
                doc.getRef(), doc.getCoreSession());
        for (DocumentModel obsoletableDoc : obsoletableDocList) {
            String versionRealUid = obsoletableDoc.getId();
            String versionLabel = obsoletableDoc.getVersionLabel();
            versionMap.put(versionRealUid, versionLabel);
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
}
