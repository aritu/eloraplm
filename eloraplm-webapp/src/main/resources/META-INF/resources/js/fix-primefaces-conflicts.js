// Fix conflicts between different versions of jQuery:
// Set RichFaces old version (1.8.3) for $ and jQuery.
// Use PFJS alias for PrimeFaces new version.

var RFJQ;
jQuery(document).ready(function() {
    RFJQ = jQuery.noConflict(true);
    $ = RFJQ;
    jQuery = RFJQ;
});
