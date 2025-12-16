package com.aritu.eloraplm.bom.characteristics.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;

@Name(BomPartCharacteristicsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomPartCharacteristicsTreeBean extends BomCharacteristicsTree {

    public static final String NAME = "bomPartCharacteristicsTreeBean";

    private static final long serialVersionUID = 1L;

    public BomPartCharacteristicsTreeBean() {
        super(EloraDoctypeConstants.BOM_PART);
    }

}