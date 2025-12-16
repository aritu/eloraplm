/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.webapp.clipboard.beans;

import static org.jboss.seam.ScopeType.SESSION;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;

/**
 * This class overrides some functionalities of Nuxeo ClipboardActionsBean.
 *
 * @author aritu
 *
 */
@Name("clipboardActions")
@Scope(SESSION)
@Install(precedence = Install.DEPLOYMENT)
public class EloraClipboardActionsBean extends ClipboardActionsBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(EloraClipboardActionsBean.class);

	/**
	 * Checks if copy action is available in the context of the current Document.
	 * <p>
	 * Nuxeo Condition: the list of selected documents is not empty.
	 * <p>
	 * Elora Condition: document is a Folder, OrderedFolder or has BomDocument,
	 * CadDocument or BasicDocument facet.
	 * <p>
	 * Note: for the instance, documents under Admin Root cannot be copied.
	 */
	@Override
	public boolean getCanCopy() {
		String logInitMsg = "[getCanCopy] [" + documentManager.getPrincipal().getName() + "] ";
		log.trace(logInitMsg + "--- ENTER ---");

		// Call super class condition (Nuxeo condition)
		boolean canCopy = super.getCanCopy();

		// Add Elora condition
		if (canCopy) {
			final List<DocumentModel> selectedDocs = documentsListsManager
					.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

			for (DocumentModel selectedDoc : selectedDocs) {

				canCopy = getCanPutInClipboard(selectedDoc);

				if (canCopy == false) {
					// exit from loop
					break;
				}
			}
		}

		log.trace(logInitMsg + "--- EXIT with canCopy = |" + canCopy + "|---");

		return canCopy;
	}

	public boolean getCanPutInClipboard() {
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		return getCanPutInClipboard(currentDocument);
	}

	public boolean getCanPutInClipboard(DocumentModel document) {

		// Documents under ADMIN ROOT cannot be copied for the instance
		DocumentRef[] documentAncestorsRefs = documentManager.getParentDocumentRefs(document.getRef());
		if (isTypeContainedInAncestors(EloraDoctypeConstants.ADMIN_ROOT, documentAncestorsRefs)) {
			return false;
		}

		// check doc type
		if (document.getType().equals(NuxeoDoctypeConstants.FOLDER)
				|| document.getType().equals(NuxeoDoctypeConstants.ORDERED_FOLDER)) {
			return true;
		}
		// check doc facet
		else if (document.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
				|| document.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
				|| document.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
			return true;
		} else {
			return false;
		}

	}

	@Override
	public boolean getCanPaste(String listName) {
		DocumentModel currentDocument = navigationContext.getCurrentDocument();

		return getCanPasteInside(listName, currentDocument);
	}

	/**
	 * Checks if the Paste action is available.
	 * <p>
	 * Nuxeo Conditions:
	 * <ul>
	 * <li>list is not empty</li>
	 * <li>user has the needed permissions on the document</li>
	 * <li>the content of the list can be added as children of the document</li>
	 * </ul>
	 *
	 * * Elora Condition:
	 * <ul>
	 * <li>For the instance, Documents can only be pasted under WorkspaceRoot
	 * structure.</li>
	 * </ul>
	 *
	 */
	@Override
	public boolean getCanPasteInside(String listName, DocumentModel document) {
		if (documentsListsManager.isWorkingListEmpty(listName) || document == null) {
			return false;
		}

		if (!documentManager.hasPermission(document.getRef(), SecurityConstants.ADD_CHILDREN)) {
			return false;
		} else {
			// filter on allowed content types
			// see if at least one doc can be pasted

			for (DocumentModel docModel : documentsListsManager.getWorkingList(listName)) {

				String pasteType = docModel.getType();

				if (typeManager.isAllowedSubType(pasteType, document.getType(),
						navigationContext.getCurrentDocument())) {

					DocumentRef[] destFolderAncestorsRefs = documentManager.getParentDocumentRefs(document.getRef());
					if (isTypeContainedInAncestors(EloraDoctypeConstants.WORKSPACE_ROOT, destFolderAncestorsRefs)) {
						return true;
					}
				}
			}
			return false;
		}

	}

	/**
	 * Checks if the Move action is available in the context of the document
	 * document. Conditions:
	 * <p>
	 * Nuxeo Conditions:
	 * <ul>
	 * <li>list is not empty
	 * <li>user has the needed permissions on the document
	 * <li>an element in the list is proxy, can be removed from its folder and added
	 * as child of the current document
	 * </ul>
	 * Elora Condition:
	 * <ul>
	 * <li>Folders and OrderedFolders can only be moved under WorkspaceRoot
	 * structure.</li>
	 * <li>Proxies of documents having Bom, Cad or Basic Document facets, can only
	 * be moved under WorkspaceRoot structure.</li>
	 * <li>Documents having Bom, Cad or Basic Document facets that are not proxies,
	 * can only be moved under Bom structure, Cad structure or Other documents
	 * structure respectively.</li>
	 * </ul>
	 */
	@Override
	public boolean getCanMoveInside(String listName, DocumentModel document) {
		if (documentsListsManager.isWorkingListEmpty(listName) || document == null) {
			return false;
		}
		DocumentRef destFolderRef = document.getRef();
		DocumentModel destFolder = document;
		if (!documentManager.hasPermission(destFolderRef, SecurityConstants.ADD_CHILDREN)) {
			return false;
		} else {
			// filter on allowed content types
			// see if at least one doc can be removed and pasted
			for (DocumentModel docModel : documentsListsManager.getWorkingList(listName)) {
				// skip deleted documents
				if (!exists(docModel.getRef())) {
					continue;
				}
				DocumentRef sourceFolderRef = docModel.getParentRef();
				String sourceType = docModel.getType();
				boolean canRemoveDoc = documentManager.hasPermission(sourceFolderRef,
						SecurityConstants.REMOVE_CHILDREN);
				boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(sourceType, destFolder.getType(),
						navigationContext.getCurrentDocument());
				boolean sameFolder = sourceFolderRef.equals(destFolderRef);
				if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {

					DocumentRef[] destFolderAncestorsRefs = documentManager.getParentDocumentRefs(destFolder.getRef());

					if (canDocumentBeMovedByDestinationAncestorType(docModel, destFolderAncestorsRefs)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	protected boolean canDocumentBeMovedByDestinationAncestorType(DocumentModel docModel,
			DocumentRef[] destFolderAncestorsRefs) {

		boolean canBeMoved = false;

		boolean isProxy = docModel.isProxy();

		if (isProxy || docModel.getType().equals(NuxeoDoctypeConstants.FOLDER)
				|| docModel.getType().equals(NuxeoDoctypeConstants.ORDERED_FOLDER)) {
			canBeMoved = isTypeContainedInAncestors(EloraDoctypeConstants.WORKSPACE_ROOT, destFolderAncestorsRefs);
		} else {
			if (docModel.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
				canBeMoved = isTypeContainedInAncestors(EloraDoctypeConstants.STRUCTURE_EBOM, destFolderAncestorsRefs);
			} else if (docModel.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
					|| docModel.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
				canBeMoved = isTypeContainedInAncestors(EloraDoctypeConstants.STRUCTURE_CAD_DOCS,
						destFolderAncestorsRefs);
			}
		}

		return canBeMoved;
	}

	protected boolean isTypeContainedInAncestors(String docType, DocumentRef[] destFolderAncestorsRefs) {

		boolean ancestorIsContained = false;

		for (int i = 0; i < destFolderAncestorsRefs.length; i++) {

			DocumentRef ancestorRef = destFolderAncestorsRefs[i];

			DocumentModel ancestorDoc = documentManager.getDocument(ancestorRef);

			if (ancestorDoc != null && ancestorDoc.getType().equals(docType)) {
				ancestorIsContained = true;

				// exit for loop
				break;
			}
		}

		return ancestorIsContained;
	}

	/**
	 * Override method (used in paste functionality)
	 * <p>
	 * Nuxeo: Creates the documents in the backend under the target parent.
	 * <p>
	 * Elora: If the document is a Folder or an OrderedFolder, copy it. Otherwise,
	 * if the document has BomDocument, CadDocument or BasicDocument facet, create a
	 * proxy of the document.
	 */
	@Override
	protected List<DocumentModel> recreateDocumentsWithNewParent(DocumentModel parent, List<DocumentModel> documents) {

		String logInitMsg = "[recreateDocumentsWithNewParent] [" + documentManager.getPrincipal().getName() + "] ";
		log.trace(logInitMsg + "--- ENTER --- ");

		List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

		if (null == parent || null == documents) {
			log.error("Null params received, returning...");
			return newDocuments;
		}

		log.trace(logInitMsg + "parent = |" + parent.getId() + "|, documents.size = |" + documents.size() + "|");

		List<DocumentModel> documentsToPast = new LinkedList<DocumentModel>();

		// filter list on content type
		for (DocumentModel doc : documents) {
			if (typeManager.isAllowedSubType(doc.getType(), parent.getType(), navigationContext.getCurrentDocument())) {
				documentsToPast.add(doc);
			}
		}

		boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(parent.getCurrentLifeCycleState());
		List<DocumentRef> docRefs = new ArrayList<DocumentRef>();

		// Elora's PASTE functionality:
		// If the document is a Folder or a OrderedFolder, paste it.
		// Otherwise, if the document has BomDocument, CadDocument or
		// BasicDocument facet, create a proxy of the document and paste it.

		StringBuilder sb = new StringBuilder();
		for (DocumentModel doc : documentsToPast) {
			if (destinationIsDeleted && !checkDeletedState(doc)) {
				addWarnMessage(sb, doc);
			}
			// if document a Folder or a OrderedFolder
			else if (doc.getType().equals(NuxeoDoctypeConstants.FOLDER)
					|| doc.getType().equals(NuxeoDoctypeConstants.ORDERED_FOLDER)) {
				docRefs.add(doc.getRef());
			}
			// if document has BomDocument, CadDocument or BasicDocument facet
			else if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
					|| doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
					|| doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {

				if (doc.isProxy()) {
					log.trace(logInitMsg + "doc = |" + doc.getId() + "| is a proxy. Retrieve it's source.");
					doc = documentManager.getDocument(new IdRef(doc.getSourceId()));
				}
				// create the proxy
				newDocuments.add(documentManager.createProxy(doc.getRef(), parent.getRef()));
				log.trace("proxy created for doc = |" + doc.getId() + "|, in target = |" + parent.getId() + "|");
			}
		}

		if (!docRefs.isEmpty()) {
			newDocuments.addAll(documentManager.copy(docRefs, parent.getRef(), true));
			log.trace("|" + docRefs.size() + "| documents pasted in target = |" + parent.getId() + "|");
		}
		if (destinationIsDeleted) {
			for (DocumentModel d : newDocuments) {
				if (!d.isProxy()) {
					setDeleteState(d);
				}
			}
		}
		documentManager.save();
		if (sb.length() > 0) {
			facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
		}

		log.trace(logInitMsg + "--- EXIT --- ");
		return newDocuments;
	}

	/**
	 * Override method (used in move functionality)
	 * <p>
	 * Elora: after moving the document, if required, update vault folder metadata
	 * in function of the destination folder.
	 */
	@Override
	public List<DocumentModel> moveDocumentsToNewParent(DocumentModel destFolder, List<DocumentModel> docs) {
		DocumentRef destFolderRef = destFolder.getRef();
		boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(destFolder.getCurrentLifeCycleState());
		List<DocumentModel> newDocs = new ArrayList<DocumentModel>();
		StringBuilder sb = new StringBuilder();
		for (DocumentModel docModel : docs) {
			DocumentRef sourceFolderRef = docModel.getParentRef();

			String sourceType = docModel.getType();
			boolean canRemoveDoc = documentManager.hasPermission(sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
			boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(sourceType, destFolder.getType(),
					navigationContext.getCurrentDocument());
			boolean sameFolder = sourceFolderRef.equals(destFolderRef);
			if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {
				if (destinationIsDeleted) {
					if (checkDeletedState(docModel)) {
						try {
							DocumentModel newDoc = documentManager.move(docModel.getRef(), destFolderRef, null);
							setDeleteState(newDoc);
							EloraStructureHelper.updateVaultFolderMetadataByDestinationFolder(newDoc, destFolder);
							newDocs.add(newDoc);
						} catch (Exception e) {
							log.error("Error while moving documents to new parent." + e.getMessage(), e);
							facesMessages.add(StatusMessage.Severity.ERROR,
									messages.get("eloraplm.message.clipboard.error.moveDocument"));
						}
					} else {
						addWarnMessage(sb, docModel);
					}
				} else {
					try {
						DocumentModel newDoc = documentManager.move(docModel.getRef(), destFolderRef, null);
						EloraStructureHelper.updateVaultFolderMetadataByDestinationFolder(newDoc, destFolder);
						newDocs.add(newDoc);
					} catch (Exception e) {
						log.error("Error while moving documents to new parent." + e.getMessage(), e);
						facesMessages.add(StatusMessage.Severity.ERROR,
								messages.get("eloraplm.message.clipboard.error.moveDocument"));
					}
				}
			}
		}
		documentManager.save();

		if (sb.length() > 0) {
			facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
		}
		return newDocs;
	}

	/*
	 * -----------------------------------------------------------------------------
	 * ------------ The following methods must be overwritten here, otherwise
	 * javascript calls don't work.
	 * -----------------------------------------------------------------------------
	 * ------------
	 */
	@Override
	@WebRemote
	public void putInClipboard(String docId) {
		super.putInClipboard(docId);
	}

	@Override
	@WebRemote
	public String pasteClipboardInside(String docId) {
		return super.pasteClipboardInside(docId);
	}

	@Override
	@WebRemote
	public String moveClipboardInside(String docId) {
		return super.moveClipboardInside(docId);
	}
	/*
	 * -----------------------------------------------------
	 */

}
