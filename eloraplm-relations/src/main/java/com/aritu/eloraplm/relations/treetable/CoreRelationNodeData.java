package com.aritu.eloraplm.relations.treetable;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.treetable.NodeData;

/**
 * @author aritu
 *
 */
public interface CoreRelationNodeData extends NodeData {

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

    Map<String, String> getVersionList();

    void setVersionList(Map<String, String> versionList);

}