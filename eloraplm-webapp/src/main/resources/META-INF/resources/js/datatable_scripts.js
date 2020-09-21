jQuery(document).ready(function() {
    
    if (typeof PrimeFaces == "undefined") {
        // PrimeFaces script is not loaded
        location.reload(true);
    }
});

var row = null;
var tableWidgetId = null;

function prepareTablePartialAjaxUpdate(caller) {
    row = jQuery('[id="' + caller.id + '"]').closest("tr");
    tableWidgetId = caller.id.split(':')[0];
    
    PF('tableBlocker_'+tableWidgetId).show();
}

function markRowAsRemoved() {
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
        }
    }
    
    PF('tableBlocker_'+tableWidgetId).hide();
}

function unmarkRowAsRemoved() {
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
    
    PF('tableBlocker_'+tableWidgetId).hide();
}


function markRowAsModified() {
    if(row != null) {
        jQuery(row)
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }
    
    PF('tableBlocker_'+tableWidgetId).hide();
}

function endRowUpdate() {
    PF('tableBlocker_'+tableWidgetId).hide();
}
