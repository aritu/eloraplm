package com.aritu.eloraplm.webapp.base.convert;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.sun.faces.util.MessageFactory;

/**
 * This class validates a given measure value.
 *
 * @author aritu
 *
 */
@Name("uniqueReferenceByTypeValidator")
@Validator
@BypassInterceptors
public class UniqueReferenceByTypeValidator
        implements javax.faces.validator.Validator {

    private static final Log log = LogFactory.getLog(
            UniqueReferenceByTypeValidator.class);

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String logInitMsg = "[validate] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        // Only check the reference uniqueness if it is not empty
        if (value == null) {
            log.trace(logInitMsg + "--- EXIT ---");
            return;
        }

        DocumentModel doc = (DocumentModel) component.getAttributes().get(
                "document");
        CoreSession session = doc.getCoreSession();
        String reference = (String) value;
        String type = doc.getType();
        String docUid = doc.isProxy() ? doc.getSourceId() : doc.getId();

        // We must run the query in an unrestricted session
        SameReferenceCounter counter = new SameReferenceCounter(session, type,
                reference, docUid);
        long docsWithSameReferenceAndType = counter.count();

        if (docsWithSameReferenceAndType > 0) {
            log.trace(logInitMsg + "Validation failed: The reference |"
                    + reference
                    + "| has already been used by another document of the same type.");

            FacesMessage message = MessageFactory.getMessage(context,
                    "eloraplm.message.error.uniqueReferenceByType", reference);

            throw new ValidatorException(message);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public class SameReferenceCounter extends UnrestrictedSessionRunner {
        String query;

        String type;

        String reference;

        String docUid;

        long count;

        public SameReferenceCounter(CoreSession session, String type,
                String reference, String docUid) {
            super(session);
            this.type = type;
            this.reference = reference;
            this.docUid = docUid;
        }

        @Override
        public void run() {
            count = EloraQueryFactory.countWcDocsByTypeAndReferenceExcludingUid(
                    session, type, reference, docUid);
        }

        public long count() {
            runUnrestricted();
            return count;
        }
    }

}
