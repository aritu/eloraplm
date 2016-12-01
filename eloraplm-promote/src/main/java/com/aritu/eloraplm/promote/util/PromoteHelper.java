package com.aritu.eloraplm.promote.util;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

public class PromoteHelper {

    private PromoteHelper() {
    }

    public static boolean checkReleasedInMajor(DocumentModel doc,
            CoreSession session) throws EloraException {
        boolean ok = false;

        DocumentModel releasedDoc = EloraDocumentHelper.getMajorReleasedVersion(
                doc, session);
        if (releasedDoc != null) {
            if (releasedDoc.getVersionLabel().equals(doc.getVersionLabel())) {
                // Doc version you chose in the tree is
                // released
                ok = true;
            }
        } else {
            // There is no released doc in this major
            // version
            ok = true;
        }
        return ok;
    }

    public static boolean checkDocumentLock(DocumentModel wcDoc,
            CoreSession session) {
        NuxeoPrincipal user = (NuxeoPrincipal) session.getPrincipal();
        if (!wcDoc.isLocked()
                || (wcDoc.isLocked() && (user.getName().equals(
                        wcDoc.getLockInfo().getOwner()) || user.isAdministrator()))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkAllowedTransition(DocumentModel doc,
            String promoteTransition) {
        boolean ok = false;
        for (String allowedTransition : doc.getAllowedStateTransitions()) {
            if (allowedTransition.equals(promoteTransition)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    public static String checkSupportedStates(DocumentModel doc,
            boolean isSpecial, String parentState, String childState,
            Statement stmt, Map<String, String> messages, CoreSession session)
            throws EloraException {
        String msg = "";
        if (isSpecial) {
            if (isSupported(childState, parentState)) {
                // Doc could have related docs that are not
                // shown in the tree. It is necessary to check
                // that doc's final state supports states of all
                // its children
                // TODO: Tendria que sacar todos los hijos ?? No
                // solo los de este predicate ?? El drawing puede tener otro
                // tipo de statements a otras piezas ??
                List<Statement> childStmts = RelationHelper.getStatements(
                        EloraRelationConstants.ELORA_GRAPH_NAME, doc,
                        stmt.getPredicate());
                for (Statement childStmt : childStmts) {
                    if (!childStmt.getObject().equals(stmt.getObject())) {
                        DocumentModel child = RelationHelper.getDocumentModel(
                                childStmt.getObject(), session);
                        if (!isSupported(childState,
                                child.getCurrentLifeCycleState())) {
                            msg = messages.get("eloraplm.message.error.state.not.supported.by.not.visible.children");
                            break;
                        }
                    }
                }
            } else {
                msg = messages.get("eloraplm.message.error.state.not.supported");
            }
        } else if (!isSupported(parentState, childState)) {
            // Check if final state is supported by parent node
            msg = messages.get("eloraplm.message.error.state.not.supported");
        }
        return msg;
    }

    public static boolean calculateAlreadyPromoted(DocumentModel doc,
            long finalStateOrdering, EloraConfigTable lifeCycleStatesConfig) {
        long stateOrdering = (long) lifeCycleStatesConfig.getRow(
                doc.getCurrentLifeCycleState()).getProperty(
                EloraConfigConstants.PROP_ORDERING);
        return stateOrdering >= finalStateOrdering;
    }

    public static boolean isSupported(String parentState, String childState)
            throws EloraException {
        boolean ok = false;
        EloraConfigTable parentSupportedStatesConfig = EloraConfigHelper.getSupportedStatesConfig(parentState);
        for (EloraConfigRow supportedConfig : parentSupportedStatesConfig.getValues()) {
            String supportedState = supportedConfig.getProperty(
                    EloraConfigConstants.PROP_LIFECYCLE_CHILDREN_STATE).toString();
            if (supportedState.equals(childState)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

}
