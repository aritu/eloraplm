/**
 *
 */
package com.aritu.eloraplm.core.archiver.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.aritu.eloraplm.exceptions.ArchivingConditionsNotMetException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface WorkspaceArchiverService {

    public static final String EXECUTER_TYPE_PRE = "pre";

    public static final String EXECUTER_TYPE_POST = "post";

    // States

    public DocumentModel archive(DocumentModel workspace)
            throws ArchivingConditionsNotMetException, EloraException,
            NuxeoException;

    public DocumentModel unarchive(DocumentModel workspace)
            throws EloraException;

    public boolean isArchiverDefinedForType(String type);

}
