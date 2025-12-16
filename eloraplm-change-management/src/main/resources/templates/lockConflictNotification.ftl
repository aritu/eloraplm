<html>
    <head>
        <style type="text/css">
            div#container
            {
                margin: 0;
                padding: 0;
                background-color: #e9ecef;
                font-family:Arial,sans-serif;
                display: flex;
                justify-content: center;
                align-content: center;
            }
            table.dataTable
            {
                border: none;
                border-spacing: 0;
                border-collapse: collapse;
                min-width: 30em;
            }
            table.dataTable td
            {
                border-top: 1px solid #ddd;
                padding: 0.2em 0.8em;
            }
            .label-error
            {
                color: #c33;
            }
        </style>
    </head>
    <body>
        <div id="container">
            <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" height="100%" style="background-color:#e9ecef;border-collapse:collapse; font-family:Arial,sans-serif;margin:0; padding:0; min-height:100% ! important; width:100% ! important;border:none;">
                <tbody>
                    <tr>
                        <td align="center" valign="top" style="border-collapse:collapse;margin:0;padding:20px;border-top:0;min-height:100%!important;width:100%!important">
                            <table cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;border:none;width:100%">
                                <tbody>
                                    <tr>
                                        <td style="background-color:#f7f7f7;border-bottom:1px dashed #e9ecef;padding:1.2em 2em;">
                                            <p style="font-weight:bold;font-size:15px;margin:0;color:#000;">
                                            ${Runtime.getProperty('org.nuxeo.ecm.product.name')}</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#fff;padding:1em 2em 0 2em;">
                                            <p>${user} erabiltzaileak ondorengo dokumentuak <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${ecoUrl}">${ecoName}</a> ECOtik desblokeatzeko jakinarazpena bidali dizu.</p>
                                            <p style="font-style:italic;">El usuario ${user} te envía una notificación para desbloquear los siguientes documentos de la ECO <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${ecoUrl}">${ecoName}</a>.</p>
                                            <p style="font-style:italic;">User ${user} sends you a notification to unlock the following documents from ECO <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${ecoUrl}">${ecoName}</a>.</p>
                                            <div>
                                                <h3>Dokumentuak / Documentos / Documents:</h3>
                                                <#list documentsNotificationInfo as docNotifInfo>
                                                    <p>
                                                        <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${docNotifInfo.docUrl}">[${docNotifInfo.doc.elora_object.reference}] ${docNotifInfo.doc.dublincore.title}</a>
                                                    </p>
                                                </#list>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f7f7f7;border-top:1px dashed #e9ecef;text-align:center;padding:0.5em 2em;">
                                            <div style="font-size:12px;color:#bbb;">${Runtime.getProperty('org.nuxeo.ecm.product.name')}</div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </body>    
</html>