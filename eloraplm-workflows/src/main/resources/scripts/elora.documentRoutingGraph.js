//jsPlumb options

var connectionColors = [ "#92e1aa", "#F7BE81", "#BDBDBD", "#5882FA", "#E1F5A9",
        "#FA5858", "#FFFF00", "#FF0000", "#D8F781" ];

var sourceEndpointOptions = {
    connector : [ "Flowchart", { cornerRadius: 5 } ],
    endpoint : [ "Dot", { radius: 4 } ],
    isSource : true,
    isTarget : false,
    uniqueEndpoint : true,
    maxConnections : 1
};

var targetEndpointOptions = {
    endpoint: "Blank",
    isSource : false,
    isTarget : true,
    reattach : true,
    // without specifying this the targetEndpoint doesn't accept multiple
    // connections
    maxConnections : -1
};

function jsPlumbInitializeDefault() {
    jsPlumb.importDefaults({
        DragOptions : {
            cursor : "pointer",
            zIndex : 2000
        },
        PaintStyle : {
            strokeStyle : "#92e1aa",
            lineWidth : 2,
            outlineWidth : 2,
            outlineColor : "white",
            joinstyle : "round"
        },
        ConnectionOverlays : [ [ "Arrow", {
            location : 1
        }, {
            fillStyle : "#92e1aa",
            width : 10
        } ] ]
    });
};

function getConnectionOverlayLabel(colour, condition) {
    return [ [ "Arrow", {
        location : 1
    }, {
        fillStyle : "#92e1aa",
        width : 10
    } ], [ "Label", {
        label : "<span title=\"" + condition + "\">" + condition + "</span>",
        cssClass : "workflow_connection_label",
        location : 
            0.4
    } ] ];
}
// --> end jsPlumbOptions

// display graph
function countElement(item, array) {
    var count = 0;
    jQuery.each(array, function(i, v) {
        if (v === item)
            count++;
    });
    return count;
};

function displayGraph(data, currentLang, divContainerTargetId) {
    jQuery.each(data['nodes'], function() {
        
        var node = '<div class="workflow_node" id="' + this.id + '"><p class="node_title tipsyShow tipsyGravityN" title="'+this.title+'">' + this.title
                + '</p></div>';
        var el = jQuery(node).appendTo('#' + divContainerTargetId).css(
                'position', 'absolute').css('left', this.x).css('top', this.y);

        // Show evaluated assignees
        if(this.assignees.length > 0) {
            var assigneesBlock = "";
            var assigneesEl = null;
            if(this.assignees.length == 1) {
                assigneesBlock = '<div class="workflow_node_assignees">' + this.assignees[0] + '</div>';
                assigneesEl = jQuery(assigneesBlock).prependTo(el);
            } else {
                var title = "";
                this.assignees.forEach( function(ass, i, assignees) {
                    title = title + ass;
                    if(i < assignees.length) {
                        title = title + "\n";
                    }
                });
                var assigneesLabel = currentLang.startsWith("eu") ? " arduradun" : (currentLang.startsWith("es") ? " responsables" : " assignees");
                assigneesBlock = '<div class="workflow_node_assignees" title="' + title + '">' + this.assignees.length + assigneesLabel + '</div>';
                assigneesEl = jQuery(assigneesBlock).prependTo(el).css("cursor", "help");
            }
        }
        
        if (this.isStartNode) {
            el.addClass('workflow_start_node');
        } else if (this.isEndNode) {
            el.addClass('workflow_end_node');
        } else if (this.isMerge) {
            // Hide label on merge nodes
            el.addClass('workflow_merge_node');
            el.empty();
        } else if (this.isMultiTask) {
            el.addClass('workflow_multiple_task');
        } else if (this.hasSubWorkflow) {
            el.addClass('workflow_subworkflow_task');
        } else {
            el.addClass('workflow_simple_task');
        }
        if (this.state == 'suspended') {
            el.addClass('workflow_node_suspended');
        }
        
        // Make it draggable
        jsPlumb.draggable(el);

    });
    // initialize connection source points
    var nodes = [];

    // determine number of source endpoints per node
    var sourceEndpoints = {};
    jQuery.each(data['transitions'], function() {
        sourceEndpoints[this.nodeSourceId] = (sourceEndpoints[this.nodeSourceId] || 0) + 1;
    });

    // transitions
    jQuery.each(data['transitions'], function() {
        var anchorIndex = countElement(this.nodeSourceId, nodes);
        if (anchorIndex > 9) {
            anchorIndex = 0;
        }
        nodes.push(this.nodeSourceId);
        var endPointSource = jsPlumb.addEndpoint(this.nodeSourceId, {
            anchor : "Continuous"
        }, sourceEndpointOptions);
        var endPointTarget = jsPlumb.addEndpoint(this.nodeTargetId, {
            anchor : "Continuous"
        }, targetEndpointOptions);

        jsPlumb.connect({
            source : endPointSource,
            target : endPointTarget,
            overlays : getConnectionOverlayLabel(connectionColors[anchorIndex],
                    this.label),
            paintStyle : {
                lineWidth : 3,
                // TODO IBAI set color by ok/ko/neutral system, with different color shades
                strokeStyle : connectionColors[anchorIndex],
                outlineWidth : 2,
                outlineColor : "white",
                joinstyle : "round"
            },
            detachable:false,
        });
    });
    jQuery(document.getElementById(divContainerTargetId)).append(
            "<input type='hidden' name='graphInitDone' value='true' />");
};

function invokeGetGraphOp(routeId, currentLang, divContainerTargetId) {
    var automationCtx = {};
    var options = {repository : ctx.repository };
    var getGraphNodesExec = jQuery().automation('Document.Routing.GetGraph', options);
    getGraphNodesExec.setContext(automationCtx);
    getGraphNodesExec.addParameter("routeDocId", routeId);
    getGraphNodesExec.addParameter("language", currentLang);
    getGraphNodesExec.executeGetBlob(function(data, status, xhr) {
        displayGraph(data, currentLang, divContainerTargetId);
    }, function(xhr, status, errorMessage) {
        jQuery('<div>Can not load graph </div>').appendTo(
                '#' + divContainerTargetId);
    }, true);
};

function loadGraph(routeDocId, currentLang, divContainerTargetId) {
    jsPlumbInitializeDefault();
    invokeGetGraphOp(routeDocId, currentLang, divContainerTargetId);
};

/** DEV function **/
function getNodeCoordinates() {
    var report = "WORKFLOW: " + document.getElementsByClassName("ui-dialog-title")[0].textContent + "\n";
    const nodes = document.getElementsByClassName("workflow_node");
    Array.prototype.forEach.call(nodes, function(n) {
        report = report + "\nNODE ID: " + n.id + "\t\t\t" +
                "X: " + n.style.left + "\t\t\t" +
                "Y: " + n.style.top;
    });
    console.log(report);
}