package com.aritu.eloraplm.core.util;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("eloraDocument")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class EloraDocumentBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    private transient EloraVersionLabelService eloraVersionLabelService;

    public String getMajorToDisplay(DocumentModel docModel) {
        return eloraVersionLabelService.getMajor(docModel);
    }

}
