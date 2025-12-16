<%
    String error = "";
    String authorizationCode = "";
    if (request.getParameter("error") != null) {
        error = request.getParameter("error");
    }
    if (request.getParameter("code") != null) {
        authorizationCode = request.getParameter("code");
    }
%>	
<html>
    <head>
    </head>
    <body>
        <p id="error"><%= error%></p>
        <p id="authorization_code"><%= authorizationCode%></p>
    </body>
</html>