var elora = elora || {}

elora.suggestbox = (function(m) {

  var absoluteUrlRegExp = /^(?:[a-z]+:)?\/\//;

  m.selectedFormatter = function(item) {
    return '';
  };

  m.suggestedFormatter = function(item) {
	  var formatted = '<span>';
	  if (item.icon) {
		  formatted = formatted.concat('<img src="/nuxeo' + item.icon + '" class="smallIcon" />');
	  }
	  if (item.reference) {
		  formatted = formatted.concat('<span class="eloraReference">' + item.reference +'</span>');
	  }
	  if (item.label) {
		  formatted = formatted.concat(item.label);
	  }
	  formatted = formatted.concat('</span>');
	  
	  return formatted;
	  
     //return '<span><img src="/nuxeo' + item.icon + '" class="smallIcon" /><span class="eloraReference">' + item.reference +'</span>' + item.label + '</span>';
  };

  m.entryHandler = function(item) {
    var docUrl = item.url;
    if (!docUrl.match(absoluteUrlRegExp)) {
      docUrl = baseURL + docUrl;
    }
    if (typeof currentConversationId != 'undefined') {
      docUrl += "?conversationId=" + currentConversationId;
    }
    window.location.replace(docUrl);
  };

  m.enterKeyHandler = function(search) {
    var searchUrl;
    window.alert("#{searchUIActions.searchPermanentLinkUrl}");
    //window.location.replace(searchUrl);
  };

  return m;

}(elora.suggestbox || {}));
