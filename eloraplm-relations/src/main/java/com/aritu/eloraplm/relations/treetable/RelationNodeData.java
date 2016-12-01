package com.aritu.eloraplm.relations.treetable;

import java.util.List;

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

    int getQuantity();

    void setQuantity(int quantity);

    String getComment();

    void setComment(String comment);

    boolean getIsObjectWc();

    void setIsObjectWc(boolean isObjectWc);

    int getOrdering();

    void setOrdering(int ordering);

    boolean getIsSpecial();

    void setIsSpecial(boolean isSpecial);

    List<String> getIconOnlyRelations();

    void setIconOnlyRelations(List<String> iconOnlyRelations);
}