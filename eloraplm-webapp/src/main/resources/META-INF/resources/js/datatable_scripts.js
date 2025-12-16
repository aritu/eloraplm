PFJQ(document).ready(function() {
    
    if (typeof PrimeFaces == "undefined") {
        // PrimeFaces script is not loaded
        location.reload(true);
    }
});



var typingTimer;

//Wait 0.3sec for long texts
var doneTypingInterval = 300;

//on keyup, start the countdown
PFJQ('table.datatable').on('keyup', 'input.blurAfterInput', function () {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(doneTyping, doneTypingInterval, this);
});

//on keydown, clear the countdown 
PFJQ('table.datatable').on('keydown', 'input.blurAfterInput', function () {
    clearTimeout(typingTimer);
});

// Wait 1sec for long texts
var doneTyping1sInterval = 1000;
//on keyup, start the countdown
PFJQ('table.datatable').on('keyup', 'input.blurAfter1sInput', function () {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(doneTyping, doneTyping1sInterval, this);
});

//on keydown, clear the countdown 
PFJQ('table.datatable').on('keydown', 'input.blurAfter1sInput', function () {
    clearTimeout(typingTimer);
});

//user is "finished typing" blur input
function doneTyping(input) {
    input.blur();
}


var row = null;
var expansionRow = null;
var tableWidgetId = null;

function prepareTablePartialAjaxUpdate(caller) {
    row = PFJQ('[id="' + caller + '"]').closest("tr");
    expansionRow = PFJQ(row).next('tr');
    if(!PFJQ(expansionRow).hasClass('ui-expanded-row-content')) {
        expansionRow = null;
    }
    
    tableWidgetId = caller.split(':')[0];
    
    PF('tableBlocker_'+tableWidgetId).show();
}

function prepareTablePartialAjaxUpdateFromExpansionTable(caller) {
    expansionRow = PFJQ('[id="' + caller + '"]').closest("tr.ui-expanded-row-content")
    row = PFJQ('[id="' + caller + '"]').closest("tr.ui-expanded-row-content").prev("tr");
    tableWidgetId = caller.split(':')[0];
    
    PF('tableBlocker_'+tableWidgetId).show();
}


function markRowAsRemoved() {
    if(row != null) {
        
        if(PFJQ(row).hasClass("rowIsNew")) {
            PFJQ(row).remove();
        } else {
            
            PFJQ(row).removeClass("rowIsNormal");
            
            if(PFJQ(row).hasClass("rowIsModified")) {
                PFJQ(row)
                    .removeClass("rowIsModified")
                    .addClass("rowBeforeModified");
            }
            
            PFJQ(row).addClass("rowIsRemoved");
        }
    }
    
    PF('tableBlocker_'+tableWidgetId).hide();
}

function unmarkRowAsRemoved() {
    if(row != null) {
        PFJQ(row).removeClass("rowIsRemoved");
        
        if(PFJQ(row).hasClass("rowBeforeModified")) {
            PFJQ(row)
                .removeClass("rowBeforeModified")
                .addClass("rowIsModified");
        } else {
            PFJQ(row).addClass("rowIsNormal");
        }
    }
    
    PF('tableBlocker_'+tableWidgetId).hide();
}


function markRowAsModified() {
    if(row != null) {
        PFJQ(row)
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }
    if(expansionRow != null) {
        PFJQ(expansionRow)
        .removeClass("rowIsNormal rowIsNew rowIsRemoved")
        .addClass("rowIsModified");
    }
    
    PF('tableBlocker_'+tableWidgetId).hide();
}

function endRowUpdate() {
    PF('tableBlocker_'+tableWidgetId).hide();
}
