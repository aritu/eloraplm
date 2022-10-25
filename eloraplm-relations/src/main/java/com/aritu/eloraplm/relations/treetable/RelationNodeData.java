package com.aritu.eloraplm.relations.treetable;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.treetable.NodeData;

/**
 * @author aritu
 *
 */
public interface RelationNodeData extends NodeData {

    String getDocId();

    void setDocId(String docId);

    DocumentModel getData();

    void setData(DocumentModel data);

    DocumentModel getWcDoc();

    void setWcDoc(DocumentModel wcDoc);

    Statement getStmt();

    void setStmt(Statement stmt);

    String getPredicateUri();

    void setPredicateUri(String predicateUri);

    String getQuantity();

    void setQuantity(String quantity);

    String getComment();

    void setComment(String comment);

    Integer getOrdering();

    void setOrdering(Integer ordering);

    Integer getDirectorOrdering();

    void setDirectorOrdering(Integer directorOrdering);

    Integer getViewerOrdering();

    void setViewerOrdering(Integer viewerOrdering);

    Integer getInverseViewerOrdering();

    void setInverseViewerOrdering(Integer inverseViewerOrdering);

    Boolean getIsManual();

    void setIsManual(Boolean isManual);

    boolean getIsSpecial();

    boolean getIsDirect();

    void setIsSpecial(boolean isSpecial);

    List<String> getIconOnlyRelations();

    void setIconOnlyRelations(List<String> iconOnlyRelations);

    Map<String, String> getVersionList();

    void setVersionList(Map<String, String> versionList);

    boolean getIsExternalSource();

    void setIsExternalSource(boolean isExternalSource);

    BomListExternalData getBomListExternalData();

    void setBomListExternalData(BomListExternalData bomListExternalData);

}