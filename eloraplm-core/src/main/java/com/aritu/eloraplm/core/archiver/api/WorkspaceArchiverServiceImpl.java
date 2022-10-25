/**
 *
 */
package com.aritu.eloraplm.core.archiver.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.core.archiver.util.UnrestrictedArchiver;
import com.aritu.eloraplm.core.archiver.util.UnrestrictedUnarchiver;
import com.aritu.eloraplm.exceptions.ArchivingConditionsNotMetException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class WorkspaceArchiverServiceImpl extends DefaultComponent
        implements WorkspaceArchiverService {

    private static final Log log = LogFactory.getLog(
            WorkspaceArchiverServiceImpl.class);

    private static final String XP_ARCHIVERS = "archivers";

    private static final String XP_TYPES = "types";

    private Map<String, ArchiverDescriptor> archivers;

    private Map<String, TypeDescriptor> types;

    @Override
    public void activate(ComponentContext context) {
        archivers = new HashMap<String, ArchiverDescriptor>();
        types = new HashMap<String, TypeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        archivers = null;
        types = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_ARCHIVERS:
            ArchiverDescriptor ad = (ArchiverDescriptor) contribution;
            if (ad != null) {
                if (ad.name == null) {
                    throw new NuxeoException("Archiver sent without a name");
                }
                archivers.put(ad.name, ad);
            }
            break;
        case XP_TYPES:
            TypeDescriptor td = (TypeDescriptor) contribution;
            if (td != null) {
                if (td.name == null) {
                    throw new NuxeoException("Type sent without a name");
                }
                types.put(td.name, td);
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    @Override
    public DocumentModel archive(DocumentModel workspace)
            throws ArchivingConditionsNotMetException, EloraException,
            NuxeoException {

        if (workspace == null) {
            throw new EloraException("Workspace is null");
        }

        if (!workspace.hasFacet(EloraFacetConstants.FACET_ARCHIVABLE)) {
            throw new EloraException("Workspace is not archivable");
        }

        String type = workspace.getType();

        if (!types.containsKey(type)) {
            throw new EloraException(
                    "No archiver defined for type |" + type + "|");
        }

        String archiverName = types.get(type).archiver;
        if (!archivers.containsKey(archiverName)) {
            throw new EloraException(
                    "No archiver defined with name |" + archiverName + "|");
        }

        ArchiverDescriptor archiver = archivers.get(archiverName);

        boolean meetsConditions = true;
        if (archiver.conditions.length > 0) {
            for (ArchiverConditionDescriptor condition : archiver.conditions) {
                String conditionClass = condition.conditionClass;
                String method = condition.method;
                if (conditionClass == null || method == null) {
                    throw new EloraException(
                            "Condition must be defined by a class and a method.");
                }

                try {
                    Class<?> c = Class.forName(conditionClass);
                    Method m = c.getMethod(method, DocumentModel.class);
                    boolean result = (boolean) m.invoke(null, workspace);
                    meetsConditions = meetsConditions && result;

                } catch (ClassNotFoundException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {

                    log.error(
                            "Error when executing condition methods for archiver |"
                                    + archiver.name + "|");
                    throw new EloraException(
                            "Error when executing condition methods. Error: "
                                    + e.getClass().getName() + " - "
                                    + e.getMessage(),
                            e);
                }
            }
        }

        if (meetsConditions) {
            workspace = new UnrestrictedArchiver(workspace.getCoreSession(),
                    workspace, archiver).archive();
        } else {

            throw new ArchivingConditionsNotMetException(workspace);
        }

        return workspace;
    }

    @Override
    public DocumentModel unarchive(DocumentModel workspace)
            throws EloraException, NuxeoException {
        if (workspace == null) {
            throw new EloraException("Workspace is null");
        }

        if (!workspace.hasFacet(EloraFacetConstants.FACET_ARCHIVABLE)) {
            throw new EloraException("Workspace is not archivable");
        }

        String type = workspace.getType();

        if (!types.containsKey(type)) {
            throw new EloraException(
                    "No archiver defined for type |" + type + "|");
        }

        String archiverName = types.get(type).archiver;
        if (!archivers.containsKey(archiverName)) {
            throw new EloraException(
                    "No archiver defined with name |" + archiverName + "|");
        }

        // ArchiverDescriptor archiver = archivers.get(archiverName);

        workspace = new UnrestrictedUnarchiver(workspace.getCoreSession(),
                workspace).unarchive();

        return workspace;
    }

    @Override
    public boolean isArchiverDefinedForType(String type) {

        if (types.containsKey(type)) {
            return archivers.containsKey(types.get(type).archiver);
        }

        return false;
    }

}
