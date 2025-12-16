PFJQ(document).ready(function () {
    if (typeof PrimeFaces == "undefined") {
        // PrimeFaces script is not loaded
        location.reload(true);
    }
});

var typingTimer;

//Wait 0.3sec for long texts
var doneTypingInterval = 300;

//on keyup, start the countdown
PFJQ("table.treetable").on("keyup", "input.blurAfterInput", function () {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(doneTyping, doneTypingInterval, this);
});

//on keydown, clear the countdown
PFJQ("table.treetable").on("keydown", "input.blurAfterInput", function () {
    clearTimeout(typingTimer);
});

// Wait 1sec for long texts
var doneTyping1sInterval = 1000;
//on keyup, start the countdown
PFJQ("table.treetable").on("keyup", "input.blurAfter1sInput", function () {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(doneTyping, doneTyping1sInterval, this);
});

//on keydown, clear the countdown
PFJQ("table.treetable").on("keydown", "input.blurAfter1sInput", function () {
    clearTimeout(typingTimer);
});

//user is "finished typing" blur input
function doneTyping(input) {
    input.blur();
}

var row = null;
var treeWidgetId = null;

function preparePartialAjaxUpdate(caller) {
    row = PFJQ('[id="' + caller.id + '"]').closest("tr");
    if (row === undefined || row.length == 0) {
        // We try without id
        row = PFJQ('[id="' + caller + '"]').closest("tr");
    }
    treeWidgetId = PFJQ(row).attr("id").split(":")[0];

    PF("tblock_" + treeWidgetId).show();
}

function markTreeNodeAsRemoved() {
    if (row != null) {
        if (PFJQ(row).hasClass("rowIsNew")) {
            PFJQ(row).remove();
            var parentid = PFJQ(row).attr("id");
            // Remove children
            PFJQ('tr[id^="' + parentid + '_"]').remove();
        } else {
            PFJQ(row).removeClass("rowIsNormal");

            if (PFJQ(row).hasClass("rowIsModified")) {
                PFJQ(row)
                    .removeClass("rowIsModified")
                    .addClass("rowBeforeModified");
            }

            PFJQ(row).addClass("rowIsRemoved");

            collapseTreeNode();
        }
    }

    PF("tblock_" + treeWidgetId).hide();
}

function unmarkTreeNodeAsRemoved() {
    if (row != null) {
        PFJQ(row).removeClass("rowIsRemoved");

        if (PFJQ(row).hasClass("rowBeforeModified")) {
            PFJQ(row)
                .removeClass("rowBeforeModified")
                .addClass("rowIsModified");
        } else {
            PFJQ(row).addClass("rowIsNormal");
        }
    }

    PF("tblock_" + treeWidgetId).hide();
}

function markTreeNodeAsModified(updateChildren) {
    if (row != null) {
        PFJQ(row)
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }

    if (updateChildren) {
        var rowId = PFJQ(row).attr("id");
        PFJQ('tr[id^="' + rowId + '_"]')
            .removeClass("rowIsNormal rowIsNew rowIsRemoved")
            .addClass("rowIsModified");
    }

    PF("tblock_" + treeWidgetId).hide();
}

function collapseTreeNode() {
    var toggler = PFJQ(row).find(".ui-treetable-toggler");
    if (toggler.hasClass("ui-icon-triangle-1-s")) {
        PFJQ(toggler).trigger("click.treeTable-toggle");
    }
}
