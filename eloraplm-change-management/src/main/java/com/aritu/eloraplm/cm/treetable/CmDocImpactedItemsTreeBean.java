package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import com.aritu.eloraplm.constants.CMConstants;

@Name("cmDocImpactedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmDocImpactedItemsTreeBean extends CmImpactedItemsTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    public CmDocImpactedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_DOC);
    }

}