package com.aritu.eloraplm.pdm.promote.util;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.config.util.LifecyclesConfig;
import com.aritu.eloraplm.config.util.LifecyclesConfigHelper;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

public class PromoteHelper {

    private PromoteHelper() {
    }

    public static boolean checkReleasedAndObsoleteInMajor(DocumentModel doc,
            CoreSession session) throws EloraException {
        boolean ok = false;

        DocumentModel releasedDoc = EloraDocumentHelper.getMajorReleasedOrObsoleteVersion(
                doc);
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

    public static boolean checkDocumentLock(DocumentModel wcDoc) {
        NuxeoPrincipal user = (NuxeoPrincipal) wcDoc.getCoreSession().getPrincipal();
        if (!wcDoc.isLocked() || (wcDoc.isLocked()
                && (user.getName().equals(wcDoc.getLockInfo().getOwner())
                        || user.isAdministrator()))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkAllowedTransition(DocumentModel doc,
            String promoteTransition) {

        // TODO: ¿No se puede hacer un simple if con
        // doc.getAllowedStateTransitions().contains() ?
        boolean ok = false;
        for (String allowedTransition : doc.getAllowedStateTransitions()) {
            if (allowedTransition.equals(promoteTransition)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    // No mirar los special en un principio. Mirar solo los direct y
    // hierarchical. Luego, cuando llegues a los drawings hay que tener en
    // cuenta todas las versiones
    // para chequear los estados. La version que aparece en el arbol habra que
    // chequear con el estado final y la version que no aparece con el estado
    // que tiene. si solo tiene una version no debe dar problemas. Esto hay que
    // comprobar siempre, incluso cuando el drawing no va en la operación
    //
    // Si se parte de un drawing los del primer nivel tendrán que mirar los
    // special también!!
    //
    // Mirar si todo esta OK se puede ahorrar la segunda vuelta!!

    public static String checkSupportedStates(DocumentModel doc,
            boolean isPropagated, String treeParentDocId, boolean isSpecial,
            String parentState, String transition, List<Resource> predicateList,
            String childState, Statement stmt, Map<String, String> messages)
            throws EloraException {
        String msg = "";
        if (isSpecial) {
            msg = checkSpecialRelationSupportedStates(doc, isPropagated,
                    transition, parentState, childState, stmt, messages, msg);
        } else {
            msg = checkAllParentStatesCorrect(doc, isPropagated,
                    treeParentDocId, parentState, childState, transition,
                    predicateList, messages);
        }
        return msg;
    }

    private static String checkSpecialRelationSupportedStates(DocumentModel doc,
            boolean isPropagated, String transition, String parentState,
            String childState, Statement stmt, Map<String, String> messages,
            String msg) throws EloraException {

        if (isPropagated) {
            if (!isTransitionAllowsConfig(transition)) {
                msg = checkParentNodeAndMissingDocs(doc, parentState,
                        childState, stmt, messages, msg);
            }
        } else {
            if (!isTransitionAllowedConfig(transition)) {
                if (!EloraDocumentHelper.isSupported(childState, parentState)) {
                    msg = messages.get(
                            "eloraplm.message.error.state.not.supported");
                }
            }
        }
        return msg;
    }

    private static String checkParentNodeAndMissingDocs(DocumentModel doc,
            String parentState, String childState, Statement stmt,
            Map<String, String> messages, String msg) throws EloraException {
        if (EloraDocumentHelper.isSupported(childState, parentState)) {
            // TODO: Tendria que sacar todos los hijos ?? No
            // solo los de este predicate ?? El drawing puede tener otro
            // tipo de statements a otras piezas ??
            msg = checkTreeMissingDocSupportedStates(doc, childState, stmt,
                    messages, msg);
        } else {
            msg = messages.get("eloraplm.message.error.state.not.supported");
        }
        return msg;
    }

    private static String checkTreeMissingDocSupportedStates(DocumentModel doc,
            String childState, Statement stmt, Map<String, String> messages,
            String msg) throws EloraException {
        List<Statement> childStmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, doc,
                stmt.getPredicate());
        for (Statement childStmt : childStmts) {
            if (!childStmt.getObject().equals(stmt.getObject())) {
                DocumentModel child = RelationHelper.getDocumentModel(
                        childStmt.getObject(), doc.getCoreSession());
                if (!EloraDocumentHelper.isSupported(childState,
                        child.getCurrentLifeCycleState())) {
                    msg = messages.get(
                            "eloraplm.message.error.state.not.supported.by.not.visible.children");
                    break;
                }
            }
        }
        return msg;
    }

    private static String checkAllParentStatesCorrect(DocumentModel doc,
            boolean isPropagated, String treeParentDocId,
            String treeParentState, String childState, String transition,
            List<Resource> predicateList, Map<String, String> messages)
            throws EloraException {

        String msg = "";
        if (isPropagated) {
            if (!isTransitionAllowedConfig(transition)) {
                if (!EloraDocumentHelper.isSupported(treeParentState,
                        childState)) {
                    msg = messages.get(
                            "eloraplm.message.error.state.not.supported");
                } else if (!areRelatedParentStatesCompatible(doc,
                        treeParentDocId, childState, predicateList)) {
                    msg = messages.get(
                            "eloraplm.message.error.state.not.supported.by.not.visible.parent");
                }
            }
        } else {
            if (!isTransitionAllowsConfig(transition)) {
                if (!EloraDocumentHelper.isSupported(treeParentState,
                        childState)) {
                    msg = messages.get(
                            "eloraplm.message.error.state.not.supported");
                }
            }
        }
        return msg;
    }

    private static boolean isTransitionAllowedConfig(String transition)
            throws EloraException {
        /*
         * EloraConfigTable transitionConfigs =
         * LifecyclesConfigHelper.getTransitionConfig( transition, false);
         * boolean transitionAllowedByAllStates = false; for (EloraConfigRow
         * transitionConfig : transitionConfigs.getValues()) { String
         * allowedByAll = transitionConfig.getProperty(
         * EloraConfigConstants.PROP_ALLOWED_BY_ALL_STATES).toString(); if
         * (allowedByAll.equals("1")) { transitionAllowedByAllStates = true; } }
         * return transitionAllowedByAllStates;
         */
        return LifecyclesConfig.allowedByAllStatesTransitionsList.contains(
                transition);
    }

    public static boolean isTransitionAllowsConfig(String transition)
            throws EloraException {
        /*
         * EloraConfigTable transitionConfigs =
         * LifecyclesConfigHelper.getTransitionConfig( transition, false);
         * boolean transitionAllowsAllStates = false; for (EloraConfigRow
         * transitionConfig : transitionConfigs.getValues()) { String allowsAll
         * = transitionConfig.getProperty(
         * EloraConfigConstants.PROP_ALLOWS_ALL_STATES).toString(); if
         * (allowsAll.equals("1")) { transitionAllowsAllStates = true; } }
         * return transitionAllowsAllStates
         */;
        return LifecyclesConfig.allowsAllStatesTransitionsList.contains(
                transition);
    }

    private static boolean areRelatedParentStatesCompatible(DocumentModel doc,
            String treeParentDocId, String childState,
            List<Resource> predicateList) throws EloraException {

        boolean areRelatedParentStatesCompatible = true;

        List<Statement> stmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                doc, predicateList);
        for (Statement stmt : stmts) {
            DocumentModel subject = RelationHelper.getDocumentModel(
                    stmt.getSubject(), doc.getCoreSession());
            if (!subject.getId().equals(treeParentDocId)) {
                if (!EloraDocumentHelper.isSupported(
                        subject.getCurrentLifeCycleState(), childState)) {
                    areRelatedParentStatesCompatible = false;
                    break;
                }
            }
        }
        return areRelatedParentStatesCompatible;
    }

    public static boolean isAlreadyPromoted(DocumentModel doc,
            String finalState, EloraConfigTable lifeCycleStatesConfig) {

        long finalStateOrdering = (long) lifeCycleStatesConfig.getRow(
                finalState).getProperty(EloraConfigConstants.PROP_ORDERING);
        long stateOrdering = (long) lifeCycleStatesConfig.getRow(
                doc.getCurrentLifeCycleState()).getProperty(
                        EloraConfigConstants.PROP_ORDERING);
        return stateOrdering >= finalStateOrdering;
    }

    public static String getFinalStateFromTransition(DocumentModel doc,
            String transition) {
        LifeCycle lc = NXCore.getLifeCycleService().getLifeCycleByName(
                doc.getLifeCyclePolicy());
        if (lc != null) {
            LifeCycleTransition t = lc.getTransitionByName(transition);
            if (t != null) {
                return t.getDestinationStateName();
            }
        }
        return null;
    }

    public static boolean parentsAllowTransition(DocumentModel doc,
            String transition, List<Resource> predicateList)
            throws EloraException {
        if (!isTransitionAllowedConfig(transition)) {
            String finalState = PromoteHelper.getFinalStateFromTransition(doc,
                    transition);

            if (!areRelatedParentStatesCompatible(doc, "", finalState,
                    predicateList)) {
                return false;
            }
        }
        return true;
    }

    /**
     * This methods checks if the document is compatible to do the promote
     * regarding the wanted final state.
     *
     * @param doc the document being promoted
     * @param finalState the wanted final state
     * @param messages map for managing translated messages
     * @return
     * @throws EloraException
     */
    public static String checkFinalStatusCompatiblity(DocumentModel doc,
            String finalState, Map<String, String> messages)
            throws EloraException {
        String msg = "";

        EloraConfigTable obsoleteAndDeletedStatesConfig = LifecyclesConfigHelper.getObsoleteAndDeletedStatesConfig();

        // For the instance, compatibility check is only required for obsolete
        // final states
        if (obsoleteAndDeletedStatesConfig.containsKey(finalState)) {
            msg = checkFinalObsoleteStatusAvailabity(doc,
                    obsoleteAndDeletedStatesConfig, messages);
        }

        return msg;
    }

    /**
     * This methods checks if the document is compatible to be promoted to an
     * obsolete state.
     *
     * @param doc the document being promoted
     * @param obsoleteAndDeletedStatesConfig obsolete and deleted states
     *            configuration
     * @param messages map for managing translated messages
     * @return
     * @throws EloraException
     */
    private static String checkFinalObsoleteStatusAvailabity(DocumentModel doc,
            EloraConfigTable obsoleteAndDeletedStatesConfig,
            Map<String, String> messages) throws EloraException {
        String msg = "";

        // if the document is a WC or the WC is based on the chosen AV,
        // check that all its versions (except the base one) are already
        // in an obsolte state
        CoreSession session = doc.getCoreSession();

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);

        if (!doc.isImmutable() || doc.getId().equals(baseDoc.getId())) {

            // get all versions of the document
            List<DocumentModel> docVersions = doc.getCoreSession().getVersions(
                    doc.getRef());

            for (DocumentModel docVersion : docVersions) {

                // the base version in excluded from the test.
                if (docVersion.getId() != baseDoc.getId()) {

                    String versionState = docVersion.getCurrentLifeCycleState();
                    if (!obsoleteAndDeletedStatesConfig.containsKey(
                            versionState)) {
                        msg = messages.get(
                                "eloraplm.message.error.promote.obsoleteStatusNotCompatible");
                    }
                }
            }
        }

        return msg;
    }

}
