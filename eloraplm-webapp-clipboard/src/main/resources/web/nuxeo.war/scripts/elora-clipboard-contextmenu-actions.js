
function doPasteAsProxy(docid)
{
    Seam.Component.getInstance("clipboardActions").pasteClipboardInsideAsProxy(docid,refreshPage);
}

function doPasteAsDuplicate(docid)
{
    Seam.Component.getInstance("clipboardActions").pasteClipboardInsideAsDuplicate(docid,refreshPage);
}



