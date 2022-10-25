jQuery(document).ready(function() {
    
    if (typeof PrimeFaces == "undefined") {
        // PrimeFaces script is not loaded
        location.reload(true);
    }
});


var typingTimer;
var doneTypingInterval = 300;

//on keyup, start the countdown
jQuery('table.treetable').on('keyup', 'input.blurAfterInput', function () {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(doneTyping, doneTypingInterval, this);
});

//on keydown, clear the countdown 
jQuery('table.treetable').on('keydown', 'input.blurAfterInput', function () {
    clearTimeout(typingTimer);
});

//user is "finished typing" blur input
function doneTyping(input) {
    input.blur();
}


var row = null;
var treeWidgetId = null;

function preparePartialAjaxUpdate(caller) {
    row = jQuery('[id="' + caller.id + '"]').closest("tr");
    treeWidgetId = jQuery(row).attr('id').split(':')[0];
    
    PF('treeBlocker_'+treeWidgetId).show();
}

function markTreeNodeAsRemoved() {
    if(row != null) {
        
        if(jQuery(row).hasClass("rowIsNew")) {
            jQuery(row).remove();
        } else {
            
            jQuery(row).removeClass("rowIsNormal");
            
            if(jQuery(row).hasClass("rowIsModified")) {
                jQuery(row)
                    .removeClass("rowIsModified")
                    .addClass("rowBeforeModified");
            }
            
            jQuery(row).addClass("rowIsRemoved");
            
            collapseTreeNode();
        }
    }
    
    PF('treeBlocker_'+treeWidgetId).hide();
}

function unmarkTreeNodeAsRemoved() {
    if(row != null) {
        jQuery(row).removeClass("rowIsRemoved");
        
        if(jQuery(row).hasClass("rowBeforeModified")) {
            jQuery(row)
                .removeClass("rowBeforeModified")
                .addClass("rowIsModified");
        } else {
            jQuery(row).addClass("rowIsNormal");
        }
    }
    
    PF('treeBlocker_'+treeWidgetId).hide();
}


function markTreeNodeAsModified(updateChildren) {
    if(row != null) {
        jQuery(row)
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }
    
    if(updateChildren) {
        var rowId = jQuery(row).attr('id');
        jQuery('tr[id^="' + rowId + '_"]')
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }
    
    PF('treeBlocker_'+treeWidgetId).hide();
}


function collapseTreeNode() {
    var toggler = jQuery(row).find('.ui-treetable-toggler');
    if(toggler.hasClass('ui-icon-triangle-1-s')) {
        jQuery(toggler).trigger('click.treeTable-toggle');
    }
}
