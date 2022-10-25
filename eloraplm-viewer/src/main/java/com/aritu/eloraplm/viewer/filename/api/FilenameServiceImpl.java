/**
 *
 */
package com.aritu.eloraplm.viewer.filename.api;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.dataevaluator.api.DataObtainerAdapter;
import com.aritu.eloraplm.viewer.dataevaluator.util.ConditionEvaluatorHelper;
import com.aritu.eloraplm.viewer.dataevaluator.util.PostProcessingHelper;
import com.aritu.eloraplm.viewer.dataevaluator.util.ValueObtainerHelper;

/**
 * @author aritu
 *
 */
public class FilenameServiceImpl extends DefaultComponent
        implements FilenameService {

    private static Log log = LogFactory.getLog(FilenameServiceImpl.class);

    private static final String XP_FILENAMES = "filenames";

    private Map<String, FilenameDescriptor> filenames;

    @Override
    public void activate(ComponentContext context) {
        filenames = new HashMap<String, FilenameDescriptor>();

    }

    @Override
    public void deactivate(ComponentContext context) {
        filenames = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        log.trace("[registerContribution] extensionPoint = |" + extensionPoint
                + "|");
        switch (extensionPoint) {
        case XP_FILENAMES:
            FilenameDescriptor filename = (FilenameDescriptor) contribution;
            if (filename.id != null) {
                filenames.put(filename.id, filename);
            } else {
                throw new NuxeoException("Filename sent without an id");
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    @Override
    public FilenameDescriptor getFilenameDescriptor(String id)
            throws EloraException {
        if (id == null || id.isEmpty()) {
            throw new EloraException("Provided filename id is null or empty.");
        }

        if (filenames != null && filenames.containsKey(id)) {
            return filenames.get(id);
        }

        return null;
    }

    @Override
    public String generateFilename(DocumentModel doc,
            String filenameDescriptorId, String action) {

        String filename = null;

        String logInitMsg = "[generateFilename] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Generating filename for doc = |" + doc.getId()
                + "| and filenameDescriptorId = |" + filenameDescriptorId
                + "|");

        try {
            FilenameDescriptor fnd = getFilenameDescriptor(
                    filenameDescriptorId);

            if (fnd == null) {
                log.error(logInitMsg + "No filenameDescriptor defined for |"
                        + filenameDescriptorId + "|");
                return filename;
            }

            filename = generateFilename(doc, fnd, action);

            log.trace(logInitMsg + "Generated filename = |" + filename + "|");

        } catch (Exception e) {
            log.error(
                    "An error occurred while generating the filename for doc |"
                            + doc.getId()
                            + "|. Returning empty filename. Error message: "
                            + e.getMessage(),
                    e);
        }

        return filename;
    }

    private String generateFilename(DocumentModel doc, FilenameDescriptor fnd,
            String action) throws EloraException {
        String filename = "";

        if (fnd.nameParts != null && fnd.nameParts.length > 0) {

            for (NamePartDescriptor npd : fnd.nameParts) {
                String namePart = processNamePart(doc, npd, action);
                filename += namePart;
            }
        }

        return filename;
    }

    private String processNamePart(DocumentModel doc, NamePartDescriptor npd,
            String action) throws EloraException {
        String namePart = "";

        if (ConditionEvaluatorHelper.fulfillsConditions(doc, action,
                npd.conditions, npd.allConditionsRequired)) {

            namePart = getNamePartValue(doc, npd, action);

        }

        return namePart;
    }

    private String getNamePartValue(DocumentModel doc, NamePartDescriptor npd,
            String action) throws EloraException {

        DataObtainerAdapter doa = new DataObtainerAdapter(npd);
        String value = ValueObtainerHelper.getValue(doc, action, doa);
        if (npd.postProcessor != null) {
            value = PostProcessingHelper.callPostProcessor(npd.postProcessor,
                    value);
        }
        return value;
    }

}
