
/* Functions called from Nuxeo actions to show the dialog */

function onPfDialogActionStart() {
    PF('globalBlocker').show();
}

function onPfDialogActionComplete(modal) {
    if(!modal) {
        PF('falseModal').show();
    }
    PF('globalBlocker').hide();
    PF('glb_pf_dlg').show();
}

/* Primefaces Dialog event listeners */

function onPfDialogShow(dialog) {
}

function onPfDialogHide(dialog, modal) {
    if(!modal) {
        PF('falseModal').hide();
    }
    emptyPfDialogActions();
}

/* Functions that control dialog's buttons ajax calls */

function handlePfDialogStart(dialog) {
    PFJQ('#' + dialog.id + '_error_msg').hide();
    PF(dialog.id + '_blocker').show();
}

function handlePfDialogSuccess(xhr, status, args, dialog) {
    if(status == "success" && !args.validationFailed) {
        PF(dialog.id).hide();
    } else {
        PF(dialog.id+'_blocker').hide();
        if(!args.validationFailed) {
            PFJQ('#' + dialog.id + '_error_msg').show();
        }
    }
}

function handlePfDialogSuccessAndReloadAll(xhr, status, args, dialog) {
    if(status == "success" && !args.validationFailed) {
        PF(dialog.id).hide();
        updateNxMainContainer();
    } else {
        PF(dialog.id+'_blocker').hide();
        if(!args.validationFailed) {
            PFJQ('#' + dialog.id + '_error_msg').show();
        }
    }
}

/* Async functions to hide dialog when the call must be done as non-ajax */

async function asyncHideDialog(wait) {
    setTimeout(hideDialog, wait);
}

function hideDialog() {
    PF('glb_pf_dlg').hide()
}