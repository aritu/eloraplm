/* List Choices */

function updateChoiceId(event) {
    var value = createCleanId(event.target.value);
    var index = event.target.id.split("-").pop();
    // Get real id
    jQuery("input[name$='choice-id-"+index+"']").val(value);
}

function createCleanId(label) {
    return label
    .toLowerCase()
    .replaceAll(" ","-")
    .replace(new RegExp(/[àáâãäå]/g),"a")
    .replace(new RegExp(/ç/g),"c")
    .replace(new RegExp(/[èéêë]/g),"e")
    .replace(new RegExp(/[ìíîï]/g),"i")
    .replace(new RegExp(/ñ/g),"n")
    .replace(new RegExp(/[òóôõö]/g),"o")
    .replace(new RegExp(/[ùúûü]/g),"u")
    .replace(/[^a-z0-9\-]/gi,"")
        .substring(0,35);
}


jQuery(document).ready(function() {
    
    jQuery('body').on('change', 'select.percentageSelect', function() {
        switch(jQuery(this).val()) {
            case '0':
                jQuery(this).removeAttr('class');
                jQuery(this).addClass('percentageSelect');
                jQuery(this).addClass('lowest');
                break;
            case '10':
            case '20':
            case '30':
                jQuery(this).removeAttr('class');
                jQuery(this).addClass('percentageSelect');
                jQuery(this).addClass('low');
                break;
            case '40':
            case '50':
            case '60':
                jQuery(this).removeAttr('class');
                jQuery(this).addClass('percentageSelect');
                jQuery(this).addClass('medium');
                break;
            case '70':
            case '80':
            case '90':
                jQuery(this).removeAttr('class');
                jQuery(this).addClass('percentageSelect');
                jQuery(this).addClass('high');
                break;
            case '100':
                jQuery(this).removeAttr('class');
                jQuery(this).addClass('percentageSelect');
                jQuery(this).addClass('highest');
                break;
            }

        jQuery(this).blur();
        
    });
    
});

/* Copy text to clipboard*/

function fallbackCopyTextToClipboard(text) {
  var textArea = document.createElement("textarea");
  textArea.value = text;
  textArea.style.position="fixed";  //avoid scrolling to bottom
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  document.execCommand('copy');

  document.body.removeChild(textArea);
}
function copyTextToClipboard(text) {
  if (!navigator.clipboard) {
    fallbackCopyTextToClipboard(text);
    return;
  }
  navigator.clipboard.writeText(text);
}
