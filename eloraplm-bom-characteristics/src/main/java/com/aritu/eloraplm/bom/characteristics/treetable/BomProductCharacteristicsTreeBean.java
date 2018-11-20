package com.aritu.eloraplm.bom.characteristics.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;

@Name("bomProductCharacteristicsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomProductCharacteristicsTreeBean
        extends BomCharacteristicsTreeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public BomProductCharacteristicsTreeBean() {
        super(EloraDoctypeConstants.BOM_PRODUCT);
    }

    @Override
    @Factory(value = "bomProductCharacteristicsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}