/**
 *
 */
package com.aritu.eloraplm.securitypolicy.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.securitypolicy.util.SecurityInfo;

/**
 * @author aritu
 *
 */
public interface SecurityConfigService {

    public Map<Integer, String> getLevelsMap();

    public String getLevelLabel(Integer id);

    public String getLevelModifier(Integer id);

    public String getLevelColor(Integer id);

    public LevelDescriptor getLevelConfig(Integer id);

    public Integer getTypeLevel(String id);

    public TypeDescriptor getTypeConfig(String id);

    public void createSecurityViewer(DocumentModel doc) throws Exception;

    public DocumentModel updateSecurityInfo(DocumentModel doc,
            SecurityInfo securityInfo) throws Exception;

    public DocumentModel configureDefaultSecurityLevel(DocumentModel doc)
            throws EloraException;

}
